package com.scmt.healthy.utils;

import org.apache.commons.codec.binary.Base64;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class BASE64DecodedMultipartFile implements MultipartFile {

    private final byte[] imgContent;
    private final String header;

    public BASE64DecodedMultipartFile(byte[] imgContent, String header) {
        this.imgContent = imgContent;
        this.header = header.split(";")[0];
    }

    @Override
    public String getName() {
        return System.currentTimeMillis() + Math.random() + "." + header.split("/")[1];
    }

    @Override
    public String getOriginalFilename() {
        return System.currentTimeMillis() + (int) Math.random() * 10000 + "." + header.split("/")[1];
    }

    @Override
    public String getContentType() {
        return header.split(":")[1];
    }

    @Override
    public boolean isEmpty() {
        return imgContent == null || imgContent.length == 0;
    }

    @Override
    public long getSize() {
        return imgContent.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return imgContent;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(imgContent);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        new FileOutputStream(dest).write(imgContent);
    }

    public static MultipartFile base64ToMultipart(String base64) {
        try {
            String[] baseStrs = base64.split(",");
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] b = new byte[0];
            b = decoder.decodeBuffer(baseStrs[1]);

            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }
            return new BASE64DecodedMultipartFile(b, baseStrs[0]);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * ??????????????????Base64??????
     *
     * @param imgFile ???????????????
     * @return
     */
    public static String getImgStr(String imgFile) {
        // ???????????????????????????????????????????????????????????????Base64????????????
        InputStream in = null;
        byte[] data = null;
        // ????????????????????????
        try {
            in = new FileInputStream(imgFile);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.encodeBase64String(data);
    }


    /**
     * ???base64????????????
     *
     * @param base64   ??????base64
     * @param filePath ????????????
     */
    public static void base64ToFile(String base64, String filePath) throws Exception {
        OutputStream os = null;
        try {
            if (StringUtils.isEmpty(filePath)) {
                throw new Exception("????????????????????????");
            }
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
                os = new FileOutputStream(file);
                byte[] fileBytes = Base64.decodeBase64(base64);
                os.write(fileBytes);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != os) {
                os.close();
            }
        }
    }

    /**
     * ?????????????????????????????????
     * @param is
     * @return
     * @throws IOException
     */
    public static byte [] inputStreamToByte(InputStream is) throws IOException {
        ByteArrayOutputStream bAOutputStream = new ByteArrayOutputStream();
        int ch;
        while((ch = is.read() ) != -1){
            bAOutputStream.write(ch);
        }
        byte data [] =bAOutputStream.toByteArray();
        bAOutputStream.close();
        return data;
    }

    /**
     * BufferedImage ??????????????? base64
     * @param bufferedImage
     * @return
     */
    public static String BufferedImageToBase64(BufferedImage bufferedImage) {
        //io???
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            //????????????
            ImageIO.write(bufferedImage, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //???????????????
        byte[] bytes = baos.toByteArray();
        BASE64Encoder encoder = new BASE64Encoder();
        //?????????base64???
        String png_base64 = encoder.encodeBuffer(bytes).trim();
        //?????? \r\n
        png_base64 = png_base64.replaceAll("\n", "").replaceAll("\r", "");
        System.out.println("?????????" + "data:image/jpg;base64," + png_base64);
        return "data:image/jpg;base64," + png_base64;
    }

    /**
     * base64 ??????????????? BufferedImage
     * @param base64
     * @return
     */
    public static BufferedImage base64ToBufferedImage(String base64) {
        BASE64Decoder decoder = new sun.misc.BASE64Decoder();
        try {
            byte[] bytes1 = decoder.decodeBuffer(base64);
            ByteArrayInputStream bas = new ByteArrayInputStream(bytes1);
            return ImageIO.read(bas);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
