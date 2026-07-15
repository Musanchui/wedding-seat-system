package com.spruce.wedding.weddingseatbackend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.spruce.wedding.weddingseatbackend.common.exception.BusinessException;
import com.spruce.wedding.weddingseatbackend.entity.Photo;
import com.spruce.wedding.weddingseatbackend.entity.WeddingEvent;
import com.spruce.wedding.weddingseatbackend.mapper.PhotoMapper;
import com.spruce.wedding.weddingseatbackend.mapper.WeddingEventMapper;
import com.spruce.wedding.weddingseatbackend.service.AdminQrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminQrCodeServiceImpl implements AdminQrCodeService {

    private final WeddingEventMapper weddingEventMapper;
    private final PhotoMapper photoMapper;

    @Value("${wedding.frontend-base-url}")
    private String frontendBaseUrl;

    @Value("${wedding.upload.base-path}")
    private String uploadBasePath;

    @Value("${wedding.upload.url-prefix}")
    private String uploadUrlPrefix;

    private static final int CANVAS_WIDTH = 900;
    private static final int CANVAS_HEIGHT = 1200;
    private static final Color GOLD = new Color(204, 166, 98);
    private static final Color DEEP_RED = new Color(168, 50, 58);

    @Override
    public byte[] generateInvitationImage(Long adminId, Long eventId) {
        WeddingEvent event = getOwnedEvent(adminId, eventId);

        String guestUrl = frontendBaseUrl + "/guest/event/" + event.getSlug() + "/home";

        try {
            BufferedImage canvas = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = canvas.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 背景：柔和的粉白渐变
            GradientPaint bg = new GradientPaint(0, 0, new Color(255, 245, 240), 0, CANVAS_HEIGHT, new Color(255, 232, 232));
            g.setPaint(bg);
            g.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

            // 装饰边框：双线金色描边
            g.setColor(GOLD);
            g.setStroke(new BasicStroke(3f));
            g.draw(new RoundRectangle2D.Double(30, 30, CANVAS_WIDTH - 60, CANVAS_HEIGHT - 60, 24, 24));
            g.setStroke(new BasicStroke(1f));
            g.draw(new RoundRectangle2D.Double(42, 42, CANVAS_WIDTH - 84, CANVAS_HEIGHT - 84, 18, 18));

            int cursorY = 100;

            // 尝试嵌入管理员上传的第一张照片(圆形裁剪)，如果这场婚礼还没传过照片，或文件读取失败，直接跳过这部分，不影响整体生成
            BufferedImage photo = tryLoadFirstPhoto(event.getId());
            if (photo != null) {
                int photoSize = 300;
                BufferedImage circlePhoto = clipToCircle(photo, photoSize);
                g.drawImage(circlePhoto, (CANVAS_WIDTH - photoSize) / 2, cursorY, null);
                g.setColor(GOLD);
                g.setStroke(new BasicStroke(4f));
                g.drawOval((CANVAS_WIDTH - photoSize) / 2, cursorY, photoSize, photoSize);
                cursorY += photoSize + 40;
            } else {
                cursorY += 20;
            }

            // 新人姓名
            String coupleTitle = buildCoupleTitle(event.getGroomName(), event.getBrideName());
            g.setColor(DEEP_RED);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 46));
            drawCentered(g, coupleTitle, cursorY + 46);
            cursorY += 46 + 20;

            g.setColor(new Color(120, 120, 120));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
            drawCentered(g, "诚邀您的光临", cursorY);
            cursorY += 50;

            if (event.getEventTime() != null) {
                String timeText = event.getEventTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"));
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
                g.setColor(new Color(90, 90, 90));
                drawCentered(g, timeText, cursorY);
                cursorY += 34;
            }
            if (event.getLocation() != null && !event.getLocation().isBlank()) {
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
                drawCentered(g, event.getLocation(), cursorY);
                cursorY += 40;
            }

            cursorY += 20;

            // 二维码
            int qrSize = 380;
            BufferedImage qrImage = generateQrImage(guestUrl, qrSize);
            g.drawImage(qrImage, (CANVAS_WIDTH - qrSize) / 2, cursorY, null);
            cursorY += qrSize + 36;

            g.setColor(new Color(150, 150, 150));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
            drawCentered(g, "扫码查看电子请柬 · 在线登记选座", cursorY);

            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(canvas, "png", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new BusinessException(500, "二维码生成失败");
        }
    }

    private String buildCoupleTitle(String groomName, String brideName) {
        String groom = (groomName == null || groomName.isBlank()) ? "新郎" : groomName;
        String bride = (brideName == null || brideName.isBlank()) ? "新娘" : brideName;
        return groom + " ♥ " + bride;
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g.drawString(text, (CANVAS_WIDTH - textWidth) / 2, y);
    }

    private BufferedImage generateQrImage(String content, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    /**
     * 尝试加载这场婚礼上传的第一张照片(按sort_order排序)。
     * 直接从服务器磁盘读取文件(而不是发HTTP请求走一遍网络)，更快也更省事。
     * 任何环节失败(还没传过照片、文件路径不对、文件损坏等)都直接返回null，
     * 调用方会优雅降级成"不嵌入照片"的纯文字版本，不会导致整个二维码生成失败。
     */
    private BufferedImage tryLoadFirstPhoto(Long eventId) {
        try {
            List<Photo> photos = photoMapper.selectList(
                    Wrappers.<Photo>lambdaQuery()
                            .eq(Photo::getEventId, eventId)
                            .orderByAsc(Photo::getSortOrder)
                            .last("LIMIT 1")
            );
            if (photos.isEmpty()) {
                return null;
            }
            String url = photos.get(0).getUrl();
            if (!url.startsWith(uploadUrlPrefix)) {
                return null;
            }
            String relativePath = url.substring(uploadUrlPrefix.length());
            File file = new File(uploadBasePath, relativePath);
            if (!file.exists()) {
                return null;
            }
            return ImageIO.read(file);
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage clipToCircle(BufferedImage source, int targetSize) {
        BufferedImage scaled = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Double(0, 0, targetSize, targetSize));
        g2.drawImage(source, 0, 0, targetSize, targetSize, null);
        g2.dispose();
        return scaled;
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
