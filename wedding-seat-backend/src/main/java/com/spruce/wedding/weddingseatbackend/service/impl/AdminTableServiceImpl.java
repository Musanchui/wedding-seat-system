package com.spruce.wedding.weddingseatbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.dto.AdminSeatDetailVO;
import com.spruce.wedding.weddingseatbackend.dto.AdminTableLayoutVO;
import com.spruce.wedding.weddingseatbackend.dto.AdminTableSaveDTO;
import com.spruce.wedding.weddingseatbackend.dto.AdminVenueLayoutVO;
import com.spruce.wedding.weddingseatbackend.dto.VenueElementVO;
import com.spruce.wedding.weddingseatbackend.entity.Guest;
import com.spruce.wedding.weddingseatbackend.entity.Seat;
import com.spruce.wedding.weddingseatbackend.entity.TableInfo;
import com.spruce.wedding.weddingseatbackend.entity.VenueElement;
import com.spruce.wedding.weddingseatbackend.entity.WeddingEvent;
import com.spruce.wedding.weddingseatbackend.mapper.GuestMapper;
import com.spruce.wedding.weddingseatbackend.mapper.SeatMapper;
import com.spruce.wedding.weddingseatbackend.mapper.TableInfoMapper;
import com.spruce.wedding.weddingseatbackend.mapper.VenueElementMapper;
import com.spruce.wedding.weddingseatbackend.mapper.WeddingEventMapper;
import com.spruce.wedding.weddingseatbackend.service.AdminTableService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTableServiceImpl implements AdminTableService {

    private final WeddingEventMapper weddingEventMapper;
    private final TableInfoMapper tableInfoMapper;
    private final SeatMapper seatMapper;
    private final GuestMapper guestMapper;
    private final VenueElementMapper venueElementMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTable(Long adminId, AdminTableSaveDTO dto) {
        // 校验这场婚礼确实是当前管理员名下的
        WeddingEvent event = getOwnedEvent(adminId, dto.getEventId());

        if (dto.getId() == null) {
            createTable(event, dto);
        } else {
            updateTable(adminId, dto);
        }
    }

    private void createTable(WeddingEvent event, AdminTableSaveDTO dto) {
        checkTableNoUnique(event.getId(), dto.getTableNo(), null);

        TableInfo table = new TableInfo();
        table.setEventId(event.getId());
        table.setTableNo(dto.getTableNo());
        table.setSeatCount(dto.getSeatCount());
        table.setRemark(dto.getRemark());
        table.setPosX(dto.getPosX());
        table.setPosY(dto.getPosY());
        table.setRotation(dto.getRotation() != null ? dto.getRotation() : 0);
        table.setStatus(1);
        tableInfoMapper.insert(table);

        // 新建桌子，按座位数批量生成对应的座位记录，seat_no从1开始
        for (int seatNo = 1; seatNo <= dto.getSeatCount(); seatNo++) {
            Seat seat = new Seat();
            seat.setTableId(table.getId());
            seat.setSeatNo(seatNo);
            seat.setStatus(0);
            seatMapper.insert(seat);
        }
    }

    private void updateTable(Long adminId, AdminTableSaveDTO dto) {
        TableInfo table = tableInfoMapper.selectById(dto.getId());
        if (table == null) {
            throw new BusinessException("桌位不存在");
        }
        // 双重校验：table所属的event必须也归属当前管理员(防止直接猜tableId越权改别人的桌子)
        getOwnedEvent(adminId, table.getEventId());

        if (!dto.getTableNo().equals(table.getTableNo())) {
            checkTableNoUnique(table.getEventId(), dto.getTableNo(), table.getId());
        }

        int oldCount = table.getSeatCount();
        int newCount = dto.getSeatCount();

        if (newCount > oldCount) {
            // 扩桌：补充新增的座位记录
            for (int seatNo = oldCount + 1; seatNo <= newCount; seatNo++) {
                Seat seat = new Seat();
                seat.setTableId(table.getId());
                seat.setSeatNo(seatNo);
                seat.setStatus(0);
                seatMapper.insert(seat);
            }
        } else if (newCount < oldCount) {
            // 缩桌：要缩减掉的那部分座位号(newCount+1 到 oldCount)，如果已经有人坐了，禁止缩减
            List<Seat> seatsToRemove = seatMapper.selectList(
                    Wrappers.<Seat>lambdaQuery()
                            .eq(Seat::getTableId, table.getId())
                            .gt(Seat::getSeatNo, newCount)
            );
            boolean anyOccupied = seatsToRemove.stream().anyMatch(s -> s.getStatus() != null && s.getStatus() == 1);
            if (anyOccupied) {
                throw new BusinessException("缩减失败：被缩减的座位中已有来宾入座，请先联系来宾更换座位，或选择更大的座位数");
            }
            for (Seat seat : seatsToRemove) {
                seatMapper.deleteById(seat.getId());
            }
        }

        table.setTableNo(dto.getTableNo());
        table.setSeatCount(newCount);
        table.setRemark(dto.getRemark());
        if (dto.getPosX() != null) {
            table.setPosX(dto.getPosX());
        }
        if (dto.getPosY() != null) {
            table.setPosY(dto.getPosY());
        }
        if (dto.getRotation() != null) {
            table.setRotation(dto.getRotation());
        }
        tableInfoMapper.updateById(table);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Long adminId, Long tableId, boolean force) {
        TableInfo table = tableInfoMapper.selectById(tableId);
        if (table == null) {
            throw new BusinessException("桌位不存在");
        }
        getOwnedEvent(adminId, table.getEventId());

        List<Seat> seats = seatMapper.selectList(
                Wrappers.<Seat>lambdaQuery().eq(Seat::getTableId, tableId)
        );
        boolean anyOccupied = seats.stream().anyMatch(s -> s.getStatus() != null && s.getStatus() == 1);

        if (anyOccupied && !force) {
            throw new BusinessException("该桌还有来宾已经入座，确认要强制删除吗？（强制删除会清空这些来宾的座位信息，需要重新为他们安排座位）");
        }

        if (anyOccupied) {
            // 强制删除：把坐在这桌的来宾的seat_id清空，避免guest表留下指向已删除座位的悬空引用
            List<Long> guestIds = seats.stream()
                    .map(Seat::getGuestId)
                    .filter(id -> id != null)
                    .toList();
            if (!guestIds.isEmpty()) {
                for (Long guestId : guestIds) {
                    Guest guest = guestMapper.selectById(guestId);
                    if (guest != null) {
//                        guest.setSeatId(null);
                        guestMapper.updateById(guest);
                    }
                }
            }
        }

        seatMapper.delete(Wrappers.<Seat>lambdaQuery().eq(Seat::getTableId, tableId));
        tableInfoMapper.deleteById(tableId);
    }

    @Override
    public AdminVenueLayoutVO getVenueLayout(Long adminId, Long eventId) {
        WeddingEvent event = getOwnedEvent(adminId, eventId);

        List<VenueElement> elements = venueElementMapper.selectList(
                Wrappers.<VenueElement>lambdaQuery()
                        .eq(VenueElement::getEventId, eventId)
                        .orderByAsc(VenueElement::getSortOrder)
        );
        List<VenueElementVO> elementVOs = elements.stream()
                .map(e -> new VenueElementVO(e.getId(), e.getType(), e.getLabel(),
                        e.getPosX(), e.getPosY(), e.getWidth(), e.getHeight(), e.getRotation()))
                .toList();

        List<AdminTableLayoutVO> tableVOs = listTables(adminId, eventId);

        return new AdminVenueLayoutVO(event.getLayoutWidth(), event.getLayoutHeight(), elementVOs, tableVOs);
    }

    @Override
    public List<AdminTableLayoutVO> listTables(Long adminId, Long eventId) {
        getOwnedEvent(adminId, eventId);

        List<TableInfo> tables = tableInfoMapper.selectList(
                Wrappers.<TableInfo>lambdaQuery().eq(TableInfo::getEventId, eventId)
        );

        return tables.stream().map(table -> {
            List<Seat> seats = seatMapper.selectList(
                    Wrappers.<Seat>lambdaQuery()
                            .eq(Seat::getTableId, table.getId())
                            .orderByAsc(Seat::getSeatNo)
            );
            List<AdminSeatDetailVO> seatVOs = seats.stream().map(seat -> {
                String guestName = null;
                String guestPhone = null;
                if (seat.getGuestId() != null) {
                    Guest guest = guestMapper.selectById(seat.getGuestId());
                    if (guest != null) {
                        guestName = guest.getName();
                        guestPhone = guest.getPhone();
                    }
                }
                return new AdminSeatDetailVO(seat.getId(), seat.getSeatNo(), seat.getStatus(), guestName, guestPhone);
            }).toList();

            return new AdminTableLayoutVO(table.getId(), table.getTableNo(), table.getSeatCount(),
                    table.getRemark(), table.getPosX(), table.getPosY(), table.getRotation(), seatVOs);
        }).toList();
    }

    private void checkTableNoUnique(Long eventId, String tableNo, Long excludeTableId) {
        TableInfo existing = tableInfoMapper.selectOne(
                Wrappers.<TableInfo>lambdaQuery()
                        .eq(TableInfo::getEventId, eventId)
                        .eq(TableInfo::getTableNo, tableNo)
        );
        if (existing != null && !existing.getId().equals(excludeTableId)) {
            throw new BusinessException("桌号 " + tableNo + " 已存在，换一个桌号");
        }
    }

    private WeddingEvent getOwnedEvent(Long adminId, Long eventId) {
        WeddingEvent event = weddingEventMapper.selectById(eventId);
        if (event == null) {
            throw new BusinessException("婚礼不存在");
        }
        if (!event.getAdminId().equals(adminId)) {
            throw new BusinessException(403, "无权操作该婚礼");
        }
        return event;
    }
}
