package com.spruce.wedding.weddingseatbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.dto.AdminGuestListItemVO;
import com.spruce.wedding.weddingseatbackend.entity.Guest;
import com.spruce.wedding.weddingseatbackend.entity.Seat;
import com.spruce.wedding.weddingseatbackend.entity.TableInfo;
import com.spruce.wedding.weddingseatbackend.entity.WeddingEvent;
import com.spruce.wedding.weddingseatbackend.mapper.GuestMapper;
import com.spruce.wedding.weddingseatbackend.mapper.SeatMapper;
import com.spruce.wedding.weddingseatbackend.mapper.TableInfoMapper;
import com.spruce.wedding.weddingseatbackend.mapper.WeddingEventMapper;
import com.spruce.wedding.weddingseatbackend.service.AdminGuestService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminGuestServiceImpl implements AdminGuestService {

    private final WeddingEventMapper weddingEventMapper;
    private final GuestMapper guestMapper;
    private final SeatMapper seatMapper;
    private final TableInfoMapper tableInfoMapper;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public List<AdminGuestListItemVO> listGuests(Long adminId, Long eventId) {
        getOwnedEvent(adminId, eventId);

        List<Guest> guests = guestMapper.selectList(
                Wrappers.<Guest>lambdaQuery().eq(Guest::getEventId, eventId).orderByDesc(Guest::getRegisterTime)
        );
        if (guests.isEmpty()) {
            return List.of();
        }

        // 一次性把这场婚礼所有桌子的table_id -> tableNo映射查出来，避免在循环里一个个查数据库(N+1问题)
        List<TableInfo> tables = tableInfoMapper.selectList(
                Wrappers.<TableInfo>lambdaQuery().eq(TableInfo::getEventId, eventId)
        );
        Map<Long, String> tableNoMap = tables.stream()
                .collect(Collectors.toMap(TableInfo::getId, TableInfo::getTableNo));

        return guests.stream().map(guest -> {
            List<Seat> seats = seatMapper.selectList(
                    Wrappers.<Seat>lambdaQuery().eq(Seat::getGuestId, guest.getId()).orderByAsc(Seat::getSeatNo)
            );

            String seatsDesc = seats.stream()
                    .map(s -> tableNoMap.getOrDefault(s.getTableId(), "?") + "号桌-" + s.getSeatNo() + "号")
                    .collect(Collectors.joining("、"));

            int seatCount = seats.size();
            String displayName = seatCount > 1
                    ? guest.getName() + "及其家人" + (seatCount - 1) + "位"
                    : guest.getName();

            return new AdminGuestListItemVO(
                    guest.getId(), displayName, guest.getName(), guest.getPhone(), guest.getCategory(),
                    seatsDesc, seatCount, guest.getRegisterTime()
            );
        }).toList();
    }

    @Override
    public byte[] exportGuestListExcel(Long adminId, Long eventId) {
        List<AdminGuestListItemVO> guestList = listGuests(adminId, eventId);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("宾客名单");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"序号", "姓名（含随行人数）", "手机号", "身份类别", "座位", "座位数", "登记时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (AdminGuestListItemVO item : guestList) {
                Row row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(rowIdx);
                row.createCell(1).setCellValue(item.getDisplayName());
                row.createCell(2).setCellValue(item.getPhone());
                row.createCell(3).setCellValue(item.getCategory() != null ? item.getCategory() : "");
                row.createCell(4).setCellValue(item.getSeatsDesc().isEmpty() ? "尚未选座" : item.getSeatsDesc());
                row.createCell(5).setCellValue(item.getSeatCount());
                row.createCell(6).setCellValue(
                        item.getRegisterTime() != null ? item.getRegisterTime().format(TIME_FORMAT) : ""
                );
                rowIdx++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(500, "生成Excel文件失败");
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
