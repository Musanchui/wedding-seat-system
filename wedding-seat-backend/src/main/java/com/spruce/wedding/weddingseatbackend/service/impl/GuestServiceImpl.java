package com.spruce.wedding.weddingseatbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.dto.AutoAssignSeatDTO;
import com.spruce.wedding.weddingseatbackend.dto.EventInfoVO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterDTO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterVO;
import com.spruce.wedding.weddingseatbackend.dto.PhotoVO;
import com.spruce.wedding.weddingseatbackend.dto.RecommendTableVO;
import com.spruce.wedding.weddingseatbackend.dto.SeatSummaryVO;
import com.spruce.wedding.weddingseatbackend.dto.SeatVO;
import com.spruce.wedding.weddingseatbackend.dto.TableLayoutVO;
import com.spruce.wedding.weddingseatbackend.dto.TableSummaryVO;
import com.spruce.wedding.weddingseatbackend.dto.VenueElementVO;
import com.spruce.wedding.weddingseatbackend.dto.VenueLayoutVO;
import com.spruce.wedding.weddingseatbackend.entity.Guest;
import com.spruce.wedding.weddingseatbackend.entity.Photo;
import com.spruce.wedding.weddingseatbackend.entity.Seat;
import com.spruce.wedding.weddingseatbackend.entity.TableInfo;
import com.spruce.wedding.weddingseatbackend.entity.VenueElement;
import com.spruce.wedding.weddingseatbackend.entity.WeddingEvent;
import com.spruce.wedding.weddingseatbackend.mapper.GuestMapper;
import com.spruce.wedding.weddingseatbackend.mapper.PhotoMapper;
import com.spruce.wedding.weddingseatbackend.mapper.SeatMapper;
import com.spruce.wedding.weddingseatbackend.mapper.TableInfoMapper;
import com.spruce.wedding.weddingseatbackend.mapper.VenueElementMapper;
import com.spruce.wedding.weddingseatbackend.mapper.WeddingEventMapper;
import com.spruce.wedding.weddingseatbackend.service.GuestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuestServiceImpl implements GuestService {

    private final WeddingEventMapper weddingEventMapper;
    private final PhotoMapper photoMapper;
    private final TableInfoMapper tableInfoMapper;
    private final SeatMapper seatMapper;
    private final GuestMapper guestMapper;
    private final VenueElementMapper venueElementMapper;

    @Override
    public EventInfoVO getEventInfo(String slug) {
        WeddingEvent event = getPublishedEventBySlug(slug);
        EventInfoVO vo = new EventInfoVO();
        vo.setId(event.getId());
        vo.setGroomName(event.getGroomName());
        vo.setBrideName(event.getBrideName());
        vo.setEventTime(event.getEventTime());
        vo.setLocation(event.getLocation());
        vo.setGreetingMessage(event.getGreetingMessage());
        vo.setMusicUrl(event.getMusicUrl());
        return vo;
    }

    /**
     * 根据slug查询婚礼，并校验必须是"已发布"状态才允许来宾端访问。
     * 所有来宾端接口的入口都要经过这一步，内部再用查出来的数字id去关联查询photo/table/guest等表，
     * 数字id不会暴露在URL里，只有slug会暴露给来宾。
     */
    private WeddingEvent getPublishedEventBySlug(String slug) {
        WeddingEvent event = weddingEventMapper.selectOne(
                Wrappers.<WeddingEvent>lambdaQuery().eq(WeddingEvent::getSlug, slug)
        );
        if (event == null) {
            throw new BusinessException("婚礼不存在，请检查链接是否正确");
        }
        if (event.getStatus() == null || event.getStatus() != 1) {
            throw new BusinessException("该婚礼尚未开放访问");
        }
        return event;
    }

    @Override
    public List<PhotoVO> getEventPhotos(String slug) {
        WeddingEvent event = getPublishedEventBySlug(slug);
        List<Photo> photos = photoMapper.selectList(
                Wrappers.<Photo>lambdaQuery()
                        .eq(Photo::getEventId, event.getId())
                        .orderByAsc(Photo::getSortOrder)
        );
        return photos.stream()
                .map(p -> new PhotoVO(p.getId(), p.getUrl(), p.getSortOrder()))
                .toList();
    }

    @Override
    public List<TableSummaryVO> getEventTables(String slug) {
        WeddingEvent event = getPublishedEventBySlug(slug);
        List<TableInfo> tables = tableInfoMapper.selectList(
                Wrappers.<TableInfo>lambdaQuery()
                        .eq(TableInfo::getEventId, event.getId())
                        .eq(TableInfo::getStatus, 1)
                        .orderByAsc(TableInfo::getTableNo)
        );
        return tables.stream()
                .map(t -> new TableSummaryVO(t.getId(), t.getTableNo(), t.getRemark(),
                        t.getSeatCount(), calcAvailableSeats(t)))
                .toList();
    }

    @Override
    public VenueLayoutVO getVenueLayout(String slug) {
        WeddingEvent event = getPublishedEventBySlug(slug);

        List<VenueElement> elements = venueElementMapper.selectList(
                Wrappers.<VenueElement>lambdaQuery()
                        .eq(VenueElement::getEventId, event.getId())
                        .orderByAsc(VenueElement::getSortOrder)
        );
        List<VenueElementVO> elementVOs = elements.stream()
                .map(e -> new VenueElementVO(e.getId(), e.getType(), e.getLabel(),
                        e.getPosX(), e.getPosY(), e.getWidth(), e.getHeight(), e.getRotation()))
                .toList();

        List<TableInfo> tables = tableInfoMapper.selectList(
                Wrappers.<TableInfo>lambdaQuery()
                        .eq(TableInfo::getEventId, event.getId())
                        .eq(TableInfo::getStatus, 1)
        );
        List<TableLayoutVO> tableVOs = tables.stream()
                .map(t -> new TableLayoutVO(t.getId(), t.getTableNo(), t.getRemark(),
                        t.getSeatCount(), calcAvailableSeats(t),
                        t.getPosX(), t.getPosY(), t.getRotation()))
                .toList();

        return new VenueLayoutVO(event.getLayoutWidth(), event.getLayoutHeight(), elementVOs, tableVOs);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GuestRegisterVO register(GuestRegisterDTO dto) {
        WeddingEvent event = getPublishedEventBySlug(dto.getEventSlug());
        Long eventId = event.getId();

        // 1. 先查该手机号在这场婚礼里是否已经登记过，避免重复登记
        Guest existing = guestMapper.selectOne(
                Wrappers.<Guest>lambdaQuery()
                        .eq(Guest::getEventId, eventId)
                        .eq(Guest::getPhone, dto.getPhone())
        );

        if (existing != null) {
            // 已登记过：查一下这个来宾名下已经选了哪些座位（可能0-3个）
            List<Seat> existingSeats = seatMapper.selectList(
                    Wrappers.<Seat>lambdaQuery().eq(Seat::getGuestId, existing.getId())
            );

            if (!existingSeats.isEmpty()) {
                // 已经选过至少1个座位了，直接把这些座位信息带回去，不再重新推荐
                List<SeatSummaryVO> seatSummaries = existingSeats.stream()
                        .map(s -> {
                            TableInfo table = tableInfoMapper.selectById(s.getTableId());
                            return new SeatSummaryVO(s.getId(), s.getTableId(),
                                    table != null ? table.getTableNo() : null, s.getSeatNo());
                        })
                        .toList();
                return new GuestRegisterVO(existing.getId(), null, seatSummaries);
            }

            // 之前登记过但一个座位都还没选，走推荐逻辑，guestId复用已有的，不新建记录
            RecommendTableVO recommend = recommendTable(eventId, existing.getCategory());
            return new GuestRegisterVO(existing.getId(), recommend, null);
        }

        // 2. 新来宾：插入登记记录（此时先不占座，只是登记信息+推荐一个桌，具体选座是另一个接口）
        Guest guest = new Guest();
        guest.setEventId(eventId);
        guest.setName(dto.getName());
        guest.setPhone(dto.getPhone());
        guest.setCategory(dto.getCategory());
        guestMapper.insert(guest);

        RecommendTableVO recommend = recommendTable(eventId, dto.getCategory());
        return new GuestRegisterVO(guest.getId(), recommend, null);
    }

    /**
     * 座位推荐核心逻辑：
     * 1. 有category：优先在 table_info.remark 模糊匹配该类别、且还有空位的桌中，选空位最多的一桌
     * 2. 没匹配到（或没传category）：直接在所有启用的桌里，选空位最多的一桌
     * 3. 全部满员：返回null，Controller/前端自行处理"暂无空位"提示
     */
    private RecommendTableVO recommendTable(Long eventId, String category) {
        List<TableInfo> tables = tableInfoMapper.selectList(
                Wrappers.<TableInfo>lambdaQuery()
                        .eq(TableInfo::getEventId, eventId)
                        .eq(TableInfo::getStatus, 1)
        );

        if (tables.isEmpty()) {
            throw new BusinessException("该婚礼尚未配置桌位，请联系婚礼主人");
        }

        // 优先按category模糊匹配remark
        if (category != null && !category.isBlank()) {
            Optional<TableInfo> matched = tables.stream()
                    .filter(t -> t.getRemark() != null && t.getRemark().contains(category))
                    .filter(t -> calcAvailableSeats(t) > 0)
                    .max(Comparator.comparingInt(this::calcAvailableSeats));
            if (matched.isPresent()) {
                TableInfo t = matched.get();
                return new RecommendTableVO(t.getId(), t.getTableNo(), t.getRemark(), calcAvailableSeats(t));
            }
        }

        // 兜底：空位最多的桌
        Optional<TableInfo> fallback = tables.stream()
                .filter(t -> calcAvailableSeats(t) > 0)
                .max(Comparator.comparingInt(this::calcAvailableSeats));

        if (fallback.isEmpty()) {
            throw new BusinessException("暂无空位，请联系婚礼主人");
        }

        TableInfo t = fallback.get();
        return new RecommendTableVO(t.getId(), t.getTableNo(), t.getRemark(), calcAvailableSeats(t));
    }

    private int calcAvailableSeats(TableInfo table) {
        Long occupied = seatMapper.selectCount(
                Wrappers.<Seat>lambdaQuery()
                        .eq(Seat::getTableId, table.getId())
                        .eq(Seat::getStatus, 1)
        );
        return table.getSeatCount() - occupied.intValue();
    }

    @Override
    public List<SeatVO> getTableSeats(Long tableId) {
        List<Seat> seats = seatMapper.selectList(
                Wrappers.<Seat>lambdaQuery()
                        .eq(Seat::getTableId, tableId)
                        .orderByAsc(Seat::getSeatNo)
        );
        return seats.stream()
                .map(s -> new SeatVO(s.getId(), s.getSeatNo(), s.getStatus(), s.getVersion()))
                .toList();
    }

    /**
     * 每位来宾最多可以锁定的座位数
     */
    private static final int MAX_SEATS_PER_GUEST = 3;

    /**
     * 选座核心：并发安全的抢座逻辑，支持一个来宾最多占3个座位（比如带家人朋友一起坐）。
     *
     * 关键点：
     * 1. seatMapper.lockSeat() 是一条原子UPDATE，WHERE条件带上 id + version + status=0，
     *    数据库层面保证"同一时刻只有一个请求能更新成功"，不依赖Java层面的锁，天然支持多实例部署。
     * 2. 返回受影响行数=0，说明version不匹配（要么已被别人抢先，要么前端缓存的version过期），
     *    直接抛SeatConflictException，Controller捕获后返回409，前端据此触发局部刷新重新选座。
     * 3. 每次锁座前先检查这个来宾当前已经占了几个座位，达到上限(3个)就拒绝，
     *    前端应该提示"最多只能选3个座位，如需换一个，请先取消其中一个"。
     * 4. 不再有"自动释放旧座位换新座位"的逻辑——现在是多选场景，选新座位不会影响已选的其他座位，
     *    如果来宾想放弃某个座位，需要调用单独的 releaseSeat 接口。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockSeat(Long guestId, Long seatId, Integer version) {
        Guest guest = guestMapper.selectById(guestId);
        if (guest == null) {
            throw new BusinessException("来宾信息不存在，请重新登记");
        }

        Long currentCount = seatMapper.selectCount(
                Wrappers.<Seat>lambdaQuery().eq(Seat::getGuestId, guestId)
        );
        if (currentCount >= MAX_SEATS_PER_GUEST) {
            throw new BusinessException("每位来宾最多只能选择" + MAX_SEATS_PER_GUEST + "个座位，如需更换，请先取消其中一个座位");
        }

        Seat targetSeat = seatMapper.selectById(seatId);
        if (targetSeat == null) {
            throw new BusinessException("座位不存在");
        }
        if (targetSeat.getStatus() != null && targetSeat.getStatus() == 1) {
            throw new BusinessException.SeatConflictException("该座位已被占用，请选择其他座位");
        }

        int affected = seatMapper.lockSeat(seatId, guestId, version);
        if (affected == 0) {
            throw new BusinessException.SeatConflictException("手速慢了一步，该座位刚被别人抢占，请重新选择");
        }
    }

    /**
     * 释放(取消)已选的某一个座位。会校验这个座位确实是这个来宾自己占的，不能取消别人的座位。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSeat(Long guestId, Long seatId) {
        Seat seat = seatMapper.selectById(seatId);
        if (seat == null) {
            throw new BusinessException("座位不存在");
        }
        if (seat.getGuestId() == null || !seat.getGuestId().equals(guestId)) {
            throw new BusinessException("这不是您选定的座位，无法取消");
        }
        seatMapper.releaseSeat(seatId, guestId);
    }

    @Override
    public List<SeatSummaryVO> getMySeats(Long guestId) {
        List<Seat> seats = seatMapper.selectList(
                Wrappers.<Seat>lambdaQuery().eq(Seat::getGuestId, guestId)
        );
        return seats.stream()
                .map(s -> {
                    TableInfo table = tableInfoMapper.selectById(s.getTableId());
                    return new SeatSummaryVO(s.getId(), s.getTableId(),
                            table != null ? table.getTableNo() : null, s.getSeatNo());
                })
                .toList();
    }

    /**
     * 不想自己选座的来宾：告诉系统要几个座位，系统在同一桌里自动挑座位号最小的几个空位分配。
     * 整个方法包在一个事务里：只要中途有一个座位抢不到（极小概率的并发冲突），
     * 就整体回滚，来宾看到的要么是"全部分配成功"，要么是"一个都没变"，不会出现分配到一半的情况。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SeatSummaryVO> autoAssignSeats(AutoAssignSeatDTO dto) {
        Guest guest = guestMapper.selectById(dto.getGuestId());
        if (guest == null) {
            throw new BusinessException("来宾信息不存在，请重新登记");
        }

        long currentCount = seatMapper.selectCount(
                Wrappers.<Seat>lambdaQuery().eq(Seat::getGuestId, dto.getGuestId())
        );
        if (currentCount + dto.getSeatCount() > 3) {
            throw new BusinessException("加上您已经选定的座位，总数不能超过3个，最多还能再分配 " + (3 - currentCount) + " 个");
        }

        TableInfo table = findTableWithCapacity(guest.getEventId(), guest.getCategory(), dto.getSeatCount());
        if (table == null) {
            throw new BusinessException("暂时没有一桌能一次容纳" + dto.getSeatCount() + "个连续空位，建议减少人数，或改为手动选座（可以分开坐在不同桌）");
        }

        List<Seat> candidates = seatMapper.selectList(
                Wrappers.<Seat>lambdaQuery()
                        .eq(Seat::getTableId, table.getId())
                        .eq(Seat::getStatus, 0)
                        .orderByAsc(Seat::getSeatNo)
                        .last("LIMIT " + dto.getSeatCount())
        );
        if (candidates.size() < dto.getSeatCount()) {
            // 极端情况：刚才查可用数时够，但这一瞬间被别人抢走了几个，兜底报错，让来宾重试
            throw new BusinessException.SeatConflictException("座位刚被别人抢占了一部分，请重新尝试分配");
        }

        List<SeatSummaryVO> result = new java.util.ArrayList<>();
        for (Seat seat : candidates) {
            int affected = seatMapper.lockSeat(seat.getId(), dto.getGuestId(), seat.getVersion());
            if (affected == 0) {
                throw new BusinessException.SeatConflictException("座位刚被别人抢占，请重新尝试分配");
            }
            result.add(new SeatSummaryVO(seat.getId(), table.getId(), table.getTableNo(), seat.getSeatNo()));
        }
        return result;
    }

    /**
     * 找一张能一次性容纳 requiredCount 个空位的桌子：优先按category匹配remark，
     * 匹配不到就在所有桌子里找空位最多的（只要空位数 >= requiredCount 就行）
     */
    private TableInfo findTableWithCapacity(Long eventId, String category, int requiredCount) {
        List<TableInfo> tables = tableInfoMapper.selectList(
                Wrappers.<TableInfo>lambdaQuery()
                        .eq(TableInfo::getEventId, eventId)
                        .eq(TableInfo::getStatus, 1)
        );

        if (category != null && !category.isBlank()) {
            Optional<TableInfo> matched = tables.stream()
                    .filter(t -> t.getRemark() != null && t.getRemark().contains(category))
                    .filter(t -> calcAvailableSeats(t) >= requiredCount)
                    .max(Comparator.comparingInt(this::calcAvailableSeats));
            if (matched.isPresent()) {
                return matched.get();
            }
        }

        return tables.stream()
                .filter(t -> calcAvailableSeats(t) >= requiredCount)
                .max(Comparator.comparingInt(this::calcAvailableSeats))
                .orElse(null);
    }

}
