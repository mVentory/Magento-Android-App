/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.detector.MonochromeRectangleDetector;
import com.google.zxing.common.detector.WhiteRectangleDetector;
import com.google.zxing.qrcode.detector.AlignmentPattern;
import com.google.zxing.qrcode.detector.Detector;
import com.mageventory.activity.MainActivity.ImageData;

public class ZXingCodeScanner {
    public static String TAG = ZXingCodeScanner.class.getSimpleName();

    private MultiFormatReader mMultiFormatReader;
    private String mLastReadCodeType;

    enum DetectType {
        QR, WHITE, MONOCHROME
    }

    public ZXingCodeScanner() {
        mMultiFormatReader = new MultiFormatReader();

        Collection<BarcodeFormat> barcodeCollection = new HashSet<BarcodeFormat>();
        barcodeCollection.add(BarcodeFormat.QR_CODE);
        barcodeCollection.add(BarcodeFormat.UPC_E);
        barcodeCollection.add(BarcodeFormat.UPC_A);
        barcodeCollection.add(BarcodeFormat.EAN_8);
        barcodeCollection.add(BarcodeFormat.EAN_13);
        barcodeCollection.add(BarcodeFormat.CODE_128);
        barcodeCollection.add(BarcodeFormat.CODE_39);
        barcodeCollection.add(BarcodeFormat.ITF);

        Map<DecodeHintType, Collection<BarcodeFormat>> barcodeFormats = new HashMap<DecodeHintType, Collection<BarcodeFormat>>();
        barcodeFormats.put(DecodeHintType.POSSIBLE_FORMATS, barcodeCollection);

        mMultiFormatReader.setHints(barcodeFormats);
    }

    public String getLastReadCodeType()
    {
        return mLastReadCodeType;
    }

    public String decode(Bitmap imageBitmap) {
        return decode(imageBitmap, true);
    }

    public String decode(Bitmap imageBitmap, boolean tryScaled) {
        if (imageBitmap == null) {
            return null;
        }

        String out = null;

        float width = imageBitmap.getWidth();
        float height = imageBitmap.getHeight();

        out = decodeInternal(imageBitmap);

        /*
         * Try smaller images if the QRcode can't be recognized in the
         * originally-sized one.
         */
        while (tryScaled && out == null)
        {
            width *= 0.7;
            height *= 0.7;

            /* No point in resizing down to much. */
            if (width * height > 80 * 80)
            {
                Bitmap smallerBitmap = Bitmap.createScaledBitmap(imageBitmap, (int) width,
                        (int) height, false);
                out = decodeInternal(smallerBitmap);
            }
            else
            {
                break;
            }
        }

        return out;
    }

    private String decodeInternal(Bitmap imageBitmap) {
        if (imageBitmap == null) {
            return null;
        }

        int width = imageBitmap.getWidth();
        int height = imageBitmap.getHeight();
        int[] pixels = new int[width * height];
        imageBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);

        boolean success;
        Result result = null;

        try {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            result = mMultiFormatReader.decodeWithState(bitmap);
            success = true;
        } catch (ReaderException e) {
            success = false;
        }

        if (success && result != null) {
            mLastReadCodeType = result.getBarcodeFormat().toString();
            String code = result.getText();
            Map<ResultMetadataType, ?> metadata = result.getResultMetadata();
            // append UPC_EAN_EXTENSION metadata if present to the scanned code
            // with the "-" separator
            if (metadata != null) {
                if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
                    String extension = metadata.get(ResultMetadataType.UPC_EAN_EXTENSION)
                            .toString();
                    if (!TextUtils.isEmpty(extension)) {
                        code += ScanUtils.UPC_EAN_EXTENSION_SEPARATOR + extension;
                    }
                }
            }
            return code;
        } else {
            return null;
        }
    }

    public String decode(String path) {
        Bitmap imageBitmap = BitmapFactory.decodeFile(path);
        return decode(imageBitmap);
    }

    public RectF detect(Bitmap imageBitmap, boolean tryScaled, DetectType detectType) {
        RectF res = null;
        ResultPoint[] dr = detectInternal(imageBitmap, detectType);
        float width = imageBitmap.getWidth();
        float height = imageBitmap.getHeight();
        res = getMultipliersRectFromDetectorResult(dr, width, height);
        while (tryScaled && res == null) {
            width *= 0.7;
            height *= 0.7;

            /* No point in resizing down to much. */
            if (width * height > 80 * 80) {
                Bitmap smallerBitmap = Bitmap.createScaledBitmap(imageBitmap, (int) width,
                        (int) height, false);
                dr = detectInternal(smallerBitmap, detectType);
                res = getMultipliersRectFromDetectorResult(dr, width, height);
            } else {
                break;
            }
        }
        return res;
    }

    private RectF getMultipliersRectFromDetectorResult(ResultPoint[] points, float bitmapWidth,
            float bitmapHeight) {
        if (points == null) {
            return null;
        }
        RectF result = new RectF();
        float minX = bitmapWidth;
        float maxX = 0;
        float minY = bitmapHeight;
        float maxY = 0;
        if (points.length == 3 || points[3] instanceof AlignmentPattern) {
            ResultPoint point4 = new ResultPoint(points[0].getX() + points[2].getX()
                    - points[1].getX(), points[0].getY() + points[2].getY() - points[1].getY());
            ResultPoint[] points2 = new ResultPoint[4];
            System.arraycopy(points, 0, points2, 0, 3);
            points2[3] = point4;
            points = points2;
        }
        for (ResultPoint p : points) {
            CommonUtils.debug(TAG,
                    "getMultipliersRectFromDetectorResult: processing point %1$f %2$f", p.getX(),
                    p.getY());
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            maxX = Math.max(maxX, p.getX());
            maxY = Math.max(maxY, p.getY());
        }
        result.left = minX / bitmapWidth;
        result.top = minY / bitmapHeight;
        result.right = maxX / bitmapWidth;
        result.bottom = maxY / bitmapHeight;
        float multipliersWidth = result.right - result.left;
        float multipliersHeight = result.bottom - result.top;
        float borderWidth = 0.2f;
        result.left = Math.max(0, result.left - multipliersWidth * borderWidth);
        result.right = Math.min(1, result.right + multipliersWidth * borderWidth);
        result.top = Math.max(0, result.top - multipliersHeight * borderWidth);
        result.bottom = Math.min(1, result.bottom + multipliersHeight * borderWidth);
        CommonUtils
                .debug(TAG,
                        "getMultipliersRectFromDetectorResult: multipliers rect %1$f %2$f %3$f %4$f for bitmap %5$fx%6$f",
                        result.left, result.top, result.right, result.bottom, bitmapWidth,
                        bitmapHeight);
        return result;

    }

    private ResultPoint[] detectInternal(Bitmap imageBitmap, DetectType detectType) {
        ResultPoint[] result = null;
        try {
            int width = imageBitmap.getWidth();
            int height = imageBitmap.getHeight();
            int[] pixels = new int[width * height];
            imageBitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            HybridBinarizer binarizer = new HybridBinarizer(source);
            BinaryBitmap binBitmap = new BinaryBitmap(binarizer);
            BitMatrix bm = binBitmap.getBlackMatrix();
            switch (detectType) {
                case MONOCHROME: {
                    MonochromeRectangleDetector detector = new MonochromeRectangleDetector(bm);
                    result = detector.detect();
                }
                    break;
                case QR: {
                    Detector detector = new Detector(bm);
                    result = detector.detect().getPoints();
                }
                    break;
                case WHITE: {
                    WhiteRectangleDetector detector = new WhiteRectangleDetector(bm);
                    result = detector.detect();
                }
                    break;
            }
        } catch (ReaderException e) {
        }
        return result;
    }

    /**
     * Try to detect and decode qr or bar code in the image
     * 
     * @param filePath
     * @param requiredSize
     * @return
     * @throws IOException
     */
    public DetectDecodeResult detectDecodeMultiStep(String filePath, int requiredSize)
            throws IOException {
        CommonUtils.debug(TAG, "detectDecodeMultiStep: processing file %1$s", filePath);
        DetectDecodeResult result = new DetectDecodeResult();
        ImageData id = ImageData.getImageDataForFile(new File(filePath), false);
        RectF cropRectMinus25Percent = new RectF(0.125f, 0.125f, 0.875f, 0.875f);
        RectF cropRectMinus50Percent = new RectF(0.25f, 0.25f, 0.75f, 0.75f);
        RectF cropRectMinus75Percent = new RectF(0.375f, 0.375f, 0.625f, 0.625f);
        RectF[] rects = new RectF[] {
                null, cropRectMinus25Percent, cropRectMinus50Percent, cropRectMinus75Percent
        };
        String[] rectDescs = new String[] {
                "100", "75", "50", "25"
        };
        for (int i = 0; i < rects.length; i++) {
            RectF rect = rects[i];
            String rectDesc = rectDescs[i];
            detectDecodeSingleStep(result, filePath, id, rect, rectDesc, requiredSize);

            if (result.isDecoded()) {
                break;
            }
        }
        CommonUtils.debug(TAG,
                "detectDecodeMultiStep: processed file %1$s. Detected %2$b; Decoded %3$b",
                filePath, result.isDetected(), result.isDecoded());
        return result;
    }

    private Rect getCropRect(ImageData id, RectF cropRectInMultipliers) {
        Rect result = ImageUtils.getRealCropRectForMultipliers(cropRectInMultipliers,
                id.getWidth(), id.getHeight());
        return ImageUtils.translateRect(result, id.getWidth(), id.getHeight(), id.getOrientation());
    }

    private void detectDecodeSingleStep(DetectDecodeResult result, String fileName, ImageData id,
            RectF cropRectMultipliers, String cropRectDesc, int requiredSize) {
        Rect cropRect = cropRectMultipliers == null ? null : getCropRect(id, cropRectMultipliers);
        Bitmap b = ImageUtils.decodeSampledBitmapFromFile(fileName, requiredSize, requiredSize,
                id.getOrientation(), cropRect);
        CommonUtils
                .debug(TAG,
                        "detectDecodeSingleStep: processing %1$s%% bitmap scaled to %2$dx%3$d, requested size %4$dx%5$d",
                        cropRectDesc, b.getWidth(), b.getHeight(), requiredSize, requiredSize);
        detectDecodeSingleStepForDetectType(result, fileName, id, cropRectMultipliers,
                requiredSize, b, DetectType.QR, false);
        if (result.isDecoded()) {
            return;
        }
        detectDecodeSingleStepForDetectType(result, fileName, id, cropRectMultipliers,
                requiredSize, b, DetectType.MONOCHROME, true);
        if (result.isDecoded()) {
            return;
        }
        detectDecodeSingleStepForDetectType(result, fileName, id, cropRectMultipliers,
                requiredSize, b, DetectType.WHITE, true);
        CommonUtils.debug(TAG, "Processed %1$s%% bitmap. Detected %2$b; Decoded %3$b",
                cropRectDesc, result.isDetected(), result.isDecoded());
    }

    private void detectDecodeSingleStepForDetectType(DetectDecodeResult result, String fileName,
            ImageData id, RectF cropRectMultipliers, int requiredSize, Bitmap b,
            DetectType detectType, boolean setDetectRectOnSuccessOnly) {
        CommonUtils
                .debug(TAG,
                        "detectDecodeSingleStepForDetectType: trying to detect region with detect type %1$s",
                        detectType.toString());
        RectF detectedRect = detect(b, true, detectType);
        if (detectedRect != null) {
            if (!setDetectRectOnSuccessOnly) {
                result.setDetectedRectMultipliers(detectedRect);
            }
            if (cropRectMultipliers != null) {
                detectedRect = ImageUtils.getNewCropRectMultipliers(cropRectMultipliers,
                        detectedRect);
                if (!setDetectRectOnSuccessOnly) {
                    result.setDetectedRectMultipliers(detectedRect);
                }
            }
            CommonUtils.debug(TAG,
                    "detectDecodeSingleStepForDetectType: trying to decode by detected region");
            Rect cropRect2 = getCropRect(id, detectedRect);
            Bitmap b2 = ImageUtils.decodeSampledBitmapFromFile(fileName, requiredSize,
                    requiredSize, id.getOrientation(), cropRect2);
            CommonUtils
                    .debug(TAG,
                            "detectDecodeSingleStepForDetectType: processing detected region bitmap scaled to %1$dx%2$d, requested size %3$dx%4$d",
                            b2.getWidth(), b2.getHeight(), requiredSize, requiredSize);
            result.setCode(decode(b2, true));
            if (result.isDecoded()) {
                CommonUtils
                        .debug(TAG,
                                "detectDecodeSingleStepForDetectType: code %1$s detected and decoded by region",
                                result.getCode());
                if (setDetectRectOnSuccessOnly) {
                    result.setDetectedRectMultipliers(detectedRect);
                }
                return;
            }
            CommonUtils.debug(TAG,
                    "detectDecodeSingleStepForDetectType: code detected but not yet decoded");
        }
    }

    public static class DetectDecodeResult {
        RectF mDetectedRectMultipliers;
        String mCode;

        public boolean isDecoded() {
            return mCode != null;
        }

        public boolean isDetected() {
            return mDetectedRectMultipliers != null;
        }

        public void setDetectedRectMultipliers(RectF detectedRectMultipliers) {
            this.mDetectedRectMultipliers = detectedRectMultipliers;
        }

        public RectF getDetectedRectMultipliers() {
            return mDetectedRectMultipliers;
        }

        public String getCode() {
            return mCode;
        }

        public void setCode(String mCode) {
            this.mCode = mCode;
        }
    }
}
