package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.Goods;
import com.example.smartvisionstock.entity.StorageLocation;
import com.example.smartvisionstock.repository.GoodsRepository;
import com.example.smartvisionstock.repository.StorageLocationRepository;
import com.example.smartvisionstock.service.QrCodeService;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private StorageLocationRepository storageLocationRepository;

    private static final QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

    @Override
    public byte[] generateQrCode(String content, int width, int height) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = QR_CODE_WRITER.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    @Override
    public byte[] generateGoodsQrCode(Long goodsId) {
        Optional<Goods> goodsOpt = goodsRepository.findById(goodsId);
        if (!goodsOpt.isPresent()) {
            return new byte[0];
        }
        Goods goods = goodsOpt.get();
        String content = String.format("SMARTVISION:GOODS:%d:%s", goods.getId(), goods.getCode());
        return generateQrCode(content, 200, 200);
    }

    @Override
    public byte[] generateLocationQrCode(Long locationId) {
        Optional<StorageLocation> locationOpt = storageLocationRepository.findById(locationId);
        if (!locationOpt.isPresent()) {
            return new byte[0];
        }
        StorageLocation location = locationOpt.get();
        String content = String.format("SMARTVISION:LOCATION:%d:%s", location.getId(), location.getLocationCode());
        return generateQrCode(content, 200, 200);
    }

    @Override
    public byte[] generateBatchQrCode(Long batchId) {
        String content = String.format("SMARTVISION:BATCH:%d", batchId);
        return generateQrCode(content, 200, 200);
    }

    @Override
    public List<byte[]> batchGenerateGoodsQrCodes(List<Long> goodsIds) {
        List<byte[]> qrCodes = new ArrayList<>();
        for (Long goodsId : goodsIds) {
            byte[] qrCode = generateGoodsQrCode(goodsId);
            if (qrCode.length > 0) {
                qrCodes.add(qrCode);
            }
        }
        return qrCodes;
    }

    @Override
    public List<byte[]> batchGenerateLocationQrCodes(List<Long> locationIds) {
        List<byte[]> qrCodes = new ArrayList<>();
        for (Long locationId : locationIds) {
            byte[] qrCode = generateLocationQrCode(locationId);
            if (qrCode.length > 0) {
                qrCodes.add(qrCode);
            }
        }
        return qrCodes;
    }

    @Override
    public Map<String, Object> scanQrCode(byte[] imageData) {
        Map<String, Object> result = new HashMap<>();
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                result.put("code", 400);
                result.put("message", "无法解析图片");
                return result;
            }

            String decoded = decodeQrCode(image);
            if (decoded != null) {
                result.put("code", 200);
                result.put("content", decoded);
                result.put("message", "识别成功");
                return result;
            }

            result.put("code", 400);
            result.put("message", "未识别到二维码");
        } catch (Exception e) {
            result.put("code", 400);
            result.put("message", "扫码失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> scanQrCodeBase64(String base64Image) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (base64Image == null || base64Image.isEmpty()) {
                result.put("code", 400);
                result.put("message", "图片数据为空");
                return result;
            }

            // Remove data URL prefix if present
            String pureBase64 = base64Image;
            if (base64Image.contains(",")) {
                pureBase64 = base64Image.substring(base64Image.indexOf(",") + 1);
            }

            byte[] imageData = Base64.getDecoder().decode(pureBase64);
            return scanQrCode(imageData);
        } catch (Exception e) {
            result.put("code", 400);
            result.put("message", "解码失败: " + e.getMessage());
        }
        return result;
    }

    private String decodeQrCode(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(DecodeHintType.TRY_HARDER, true);

            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }
}
