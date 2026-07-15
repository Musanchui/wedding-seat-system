package com.spruce.wedding.weddingseatbackend.controller;

import com.spruce.wedding.weddingseatbackend.common.result.Result;
import com.spruce.wedding.weddingseatbackend.dto.AutoAssignSeatDTO;
import com.spruce.wedding.weddingseatbackend.dto.EventInfoVO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterDTO;
import com.spruce.wedding.weddingseatbackend.dto.GuestRegisterVO;
import com.spruce.wedding.weddingseatbackend.dto.PhotoVO;
import com.spruce.wedding.weddingseatbackend.dto.SeatLockDTO;
import com.spruce.wedding.weddingseatbackend.dto.SeatReleaseDTO;
import com.spruce.wedding.weddingseatbackend.dto.SeatSummaryVO;
import com.spruce.wedding.weddingseatbackend.dto.SeatVO;
import com.spruce.wedding.weddingseatbackend.dto.TableSummaryVO;
import com.spruce.wedding.weddingseatbackend.dto.VenueLayoutVO;
import com.spruce.wedding.weddingseatbackend.service.GuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 来宾端接口，无需登录，对应契约里的 /api/guest/** 路径
 * (context-path=/api 已经在application.yml里全局配置，这里不用重复写)
 */
@RestController
@RequestMapping("/guest")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    /**
     * GET /api/guest/event/{slug}
     * H5欢迎页初始化加载：新人姓名、时间、地点、寄语、音乐URL。
     * slug是婚礼创建时生成的短标识（如 zhang-li-0815），来宾扫码/点链接进来的url用的是这个，
     * 不暴露数据库自增主键，防止有人靠猜数字ID看到别人的婚礼。
     */
    @GetMapping("/event/{slug}")
    public Result<EventInfoVO> getEventInfo(@PathVariable String slug) {
        return Result.success(guestService.getEventInfo(slug));
    }

    /**
     * GET /api/guest/event/{slug}/photos
     * 滚动照片墙，按sort_order升序
     */
    @GetMapping("/event/{slug}/photos")
    public Result<List<PhotoVO>> getEventPhotos(@PathVariable String slug) {
        return Result.success(guestService.getEventPhotos(slug));
    }

    /**
     * GET /api/guest/event/{slug}/tables
     * 获取该婚礼所有桌位及各桌剩余座位数，供来宾自主浏览挑选任意一桌（不局限于登记时系统推荐的那一桌）
     */
    @GetMapping("/event/{slug}/tables")
    public Result<List<TableSummaryVO>> getEventTables(@PathVariable String slug) {
        return Result.success(guestService.getEventTables(slug));
    }

    /**
     * GET /api/guest/event/{slug}/venue-layout
     * 完整场地布局图：画布尺寸 + 舞台/屏幕/出入口等场地元素坐标 + 所有桌子坐标(带剩余座位数)。
     * 前端用这个接口的数据在一个固定尺寸(canvasWidth x canvasHeight)的画布上，
     * 按每个元素/桌子的posX/posY/width/height/rotation绝对定位渲染出真实场地平面图。
     */
    @GetMapping("/event/{slug}/venue-layout")
    public Result<VenueLayoutVO> getVenueLayout(@PathVariable String slug) {
        return Result.success(guestService.getVenueLayout(slug));
    }

    /**
     * POST /api/guest/register
     * 来宾登记 + 座位推荐。重复登记（同手机号）会直接返回已有记录，不会重复插入。
     */
    @PostMapping("/register")
    public Result<GuestRegisterVO> register(@Valid @RequestBody GuestRegisterDTO dto) {
        return Result.success(guestService.register(dto));
    }

    /**
     * GET /api/guest/table/{tableId}/seats
     * 某一桌的所有座位状态，前端用来渲染选座网格（绿色空闲/红色占用），并把version存本地供选座时校验
     */
    @GetMapping("/table/{tableId}/seats")
    public Result<List<SeatVO>> getTableSeats(@PathVariable Long tableId) {
        return Result.success(guestService.getTableSeats(tableId));
    }

    /**
     * POST /api/guest/seat/lock
     * 抢座核心接口。一个来宾最多可以锁定3个座位，超过会返回普通业务错误(code=400)。
     * 乐观锁冲突时会抛SeatConflictException，由GlobalExceptionHandler统一转换成 code=409 的Result返回，
     * 前端据此触发局部刷新重选。
     */
    @PostMapping("/seat/lock")
    public Result<Boolean> lockSeat(@Valid @RequestBody SeatLockDTO dto) {
        guestService.lockSeat(dto.getGuestId(), dto.getSeatId(), dto.getVersion());
        return Result.success(true);
    }

    /**
     * POST /api/guest/seat/release
     * 取消(释放)已选的某一个座位。会校验这个座位确实是该来宾自己选的。
     */
    @PostMapping("/seat/release")
    public Result<Boolean> releaseSeat(@Valid @RequestBody SeatReleaseDTO dto) {
        guestService.releaseSeat(dto.getGuestId(), dto.getSeatId());
        return Result.success(true);
    }

    /**
     * GET /api/guest/my-seats?guestId=xxx
     * 查询某个来宾当前已经选定的所有座位(0-3个)，前端可以用这个接口展示"我的选座"汇总页
     */
    @GetMapping("/my-seats")
    public Result<List<SeatSummaryVO>> getMySeats(@RequestParam Long guestId) {
        return Result.success(guestService.getMySeats(guestId));
    }

    /**
     * POST /api/guest/seat/auto-assign
     * 不想自己选座的来宾用这个：告诉系统要几个座位(含自己，最多3个)，系统自动分配。
     */
    @PostMapping("/seat/auto-assign")
    public Result<List<SeatSummaryVO>> autoAssignSeats(@Valid @RequestBody AutoAssignSeatDTO dto) {
        return Result.success(guestService.autoAssignSeats(dto));
    }
}
