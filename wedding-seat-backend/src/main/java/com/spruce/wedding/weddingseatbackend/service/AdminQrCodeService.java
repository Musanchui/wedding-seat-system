package com.spruce.wedding.weddingseatbackend.service;

public interface AdminQrCodeService {

    /** 生成邀请函二维码图片(PNG格式字节数组) */
    byte[] generateInvitationImage(Long adminId, Long eventId);
}
