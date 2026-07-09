package com.spruce.wedding.weddingseatbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.dto.EventInfoVO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterDTO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterVO;
import com.spruce.wedding.weddingseatbackend.dto.PhotoVO;
import com.spruce.wedding.weddingseatbackend.dto.RecommendTableVO;
import com.spruce.wedding.weddingseatbackend.dto.SeatVO;
import com.spruce.wedding.weddingseatbackend.entity.Guest;
import com.spruce.wedding.weddingseatbackend.entity.Photo;
import com.spruce.wedding.weddingseatbackend.entity.Seat;
import com.spruce.wedding.weddingseatbackend.entity.TableInfo;
import com.spruce.wedding.weddingseatbackend.entity.WeddingEvent;
import com.spruce.wedding.weddingseatbackend.mapper.GuestMapper;
import com.spruce.wedding.weddingseatbackend.mapper.PhotoMapper;
import com.spruce.wedding.weddingseatbackend.mapper.SeatMapper;
import com.spruce.wedding.weddingseatbackend.mapper.TableInfoMapper;
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

    @Override
    public EventInfoVO getEventInfo(Long eventId) {
        WeddingEvent event = weddingEventMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException("婚礼不存在");
        }
        if (event.getStatus() == null || event.getStatus() != 1) {
            throw new BusinessException("该婚礼尚未开放访问");
        }
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

    @Override
    public List<PhotoVO> getEventPhotos(Long eventId) {
        List<Photo> photos = photoMapper.selectList(
                Wrappers.<Photo>lambdaQuery()
                        .eq(Photo::getEventId, eventId)
                        .orderByAsc(Photo::getSortOrder)
        );
        return photos.stream()
                .map(p -> new PhotoVO(p.getId(), p.getUrl(), p.getSortOrder()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GuestRegisterVO register(GuestRegisterDTO dto) {
        // 1. 先查该手机号在这场婚礼里是否已经登记过，避免重复登记
        Guest existing = guestMapper.selectOne(
                Wrappers.<Guest>lambdaQuery()
                        .eq(Guest::getEventId, dto.getEventId())
                        .eq(Guest::getPhone, dto.getPhone())
        );

        if (existing != null) {
            // 已登记过：如果已经选了座位，直接把座位所在桌信息带回去；如果还没选座，重新走一次推荐逻辑
            if (existing.getSeatId() != null) {
                Seat seat = seatMapper.selectById(existing.getSeatId());
                if (seat != null) {
                    TableInfo table = tableInfoMapper.selectById(seat.getTableId());
                    if (table != null) {
                        RecommendTableVO tableVO = new RecommendTableVO(
                                table.getId(), table.getTableNo(), table.getRemark(),
                                calcAvailableSeats(table)
                        );
                        return new GuestRegisterVO(existing.getId(), tableVO);
                    }
                }
            }
            // 之前登记过但还没选座，走推荐逻辑，guestId复用已有的，不新建记录
            RecommendTableVO recommend = recommendTable(dto.getEventId(), existing.getCategory());
            return new GuestRegisterVO(existing.getId(), recommend);
        }

        // 2. 新来宾：插入登记记录（此时先不占座，只是登记信息+推荐一个桌，具体选座是另一个接口）
        Guest guest = new Guest();
        guest.setEventId(dto.getEventId());
        guest.setName(dto.getName());
        guest.setPhone(dto.getPhone());
        guest.setCategory(dto.getCategory());
        guestMapper.insert(guest);

        RecommendTableVO recommend = recommendTable(dto.getEventId(), dto.getCategory());
        return new GuestRegisterVO(guest.getId(), recommend);
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
     * 选座核心：并发安全的抢座逻辑。
     *
     * 关键点：
     * 1. seatMapper.lockSeat() 是一条原子UPDATE，WHERE条件带上 id + version + status=0，
     *    数据库层面保证"同一时刻只有一个请求能更新成功"，不依赖Java层面的锁，天然支持多实例部署。
     * 2. 返回受影响行数=0，说明version不匹配（要么已被别人抢先，要么前端缓存的version过期），
     *    直接抛SeatConflictException，Controller捕获后返回409，前端据此触发局部刷新重新选座。
     * 3. 如果该来宾之前已经选了别的座位（换座场景），先释放旧座位，再抢新座位，整个过程在一个事务里，
     *    保证"要么换座完全成功，要么完全不变"，不会出现"旧座位释放了但新座位没抢到"的中间状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockSeat(Long guestId, Long seatId, Integer version) {
        Guest guest = guestMapper.selectById(guestId);
        if (guest == null) {
            throw new BusinessException("来宾信息不存在，请重新登记");
        }

        Seat targetSeat = seatMapper.selectById(seatId);
        if (targetSeat == null) {
            throw new BusinessException("座位不存在");
        }
        if (targetSeat.getStatus() != null && targetSeat.getStatus() == 1) {
            throw new BusinessException.SeatConflictException("该座位已被占用，请选择其他座位");
        }

        // 换座场景：先释放该来宾原来占用的座位
        if (guest.getSeatId() != null && !guest.getSeatId().equals(seatId)) {
            seatMapper.releaseSeat(guest.getSeatId(), guestId);
        }

        int affected = seatMapper.lockSeat(seatId, guestId, version);
        if (affected == 0) {
            throw new BusinessException.SeatConflictException("手速慢了一步，该座位刚被别人抢占，请重新选择");
        }

        guest.setSeatId(seatId);
        guestMapper.updateById(guest);
    }
}
