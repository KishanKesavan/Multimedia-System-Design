
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;


public class Analysis3WithUniformQuantization {

    JFrame frame;
    JLabel lbIm1;
    JLabel lbIm2;
    JLabel lbIm3;
    BufferedImage img,oldImg,newImg;

    public void showIms(String[] args){
        int width = Integer.parseInt(args[1]);
        int height = Integer.parseInt(args[2]);
        int quantizationFactor = 1;

        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        oldImg = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
        newImg = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);


        try {
            File file = new File(args[0]);
            InputStream is = new FileInputStream(file);

            long len = file.length();
            byte[] bytes = new byte[(int)len];

            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

            //New Procedure

            int ind = 0;
            Map<Integer,Integer> red = new HashMap<>();
            Map<Integer,Integer> green = new HashMap<>();
            Map<Integer,Integer> blue = new HashMap<>();
            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    byte r = bytes[ind];
                    byte g = bytes[ind+height*width];
                    byte b = bytes[ind+height*width*2];
                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    mapAdd(red,Byte.toUnsignedInt(r));
                    mapAdd(green,Byte.toUnsignedInt(g));
                    mapAdd(blue,Byte.toUnsignedInt(b));
                    img.setRGB(x,y,pix);
                    ind++;
                }
            }

            int sum,count;
            int[] rValues = new int[quantizationFactor];
            for(int i=0;i<quantizationFactor;++i){
                sum = 0;
                count = 0;
                for(int j=0; j<256/quantizationFactor; ++j){
                    if(red.get(i*(256/quantizationFactor)+j) != null){
                        sum += red.get(i*(256/quantizationFactor)+j) * (i*(256/quantizationFactor)+j);
                        count+= red.get(i*(256/quantizationFactor)+j);
                    }
                }
                rValues[i] = sum/count;
            }

            int[] gValues = new int[quantizationFactor];
            for(int i=0;i<quantizationFactor;++i){
                sum = 0;
                count = 0;
                for(int j=0; j<256/quantizationFactor; ++j){
                    if(green.get(i*(256/quantizationFactor)+j) != null){
                        sum += green.get(i*(256/quantizationFactor)+j) * (i*(256/quantizationFactor)+j);
                        count+= green.get(i*(256/quantizationFactor)+j);
                    }
                }
                gValues[i] = sum/count;
            }

            int[] bValues = new int[quantizationFactor];
            for(int i=0;i<quantizationFactor;++i){
                sum = 0;
                count = 0;
                for(int j=0; j<256/quantizationFactor; ++j){
                    if(blue.get(i*(256/quantizationFactor)+j) != null){
                        sum += blue.get(i*(256/quantizationFactor)+j) * (i*(256/quantizationFactor)+j);
                        count += blue.get(i*(256/quantizationFactor)+j);
                    }
                }
                bValues[i] = sum/count;
            }
            //quantizedImageCreation
            ind = 0;
            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    byte r = bytes[ind];
                    byte g = bytes[ind+height*width];
                    byte b = bytes[ind+height*width*2];
                    int pix = 0xff000000 | (getClosestFromRange(Byte.toUnsignedInt(r),rValues) << 16) | (getClosestFromRange(Byte.toUnsignedInt(g),gValues) << 8) | getClosestFromRange(Byte.toUnsignedInt(b),bValues);
                    //int pix = 0xff000000 | (rValues[Byte.toUnsignedInt(r)*quantizationFactor/256] << 16) | (gValues[Byte.toUnsignedInt(g)*quantizationFactor/256] << 8) | bValues[Byte.toUnsignedInt(b)*quantizationFactor/256];
                    newImg.setRGB(x,y,pix);
                    ind++;
                }
            }

            // Original procedure


            //ConvertToYUV
            int[][] givenYUV = convertToYUV(bytes,width,height);
            int ySubSample = 1;
            int uSubSample = 1;
            int vSubSample = 1;

            //SubSampling
            int[][] subSampledYUV = subSample(givenYUV,ySubSample,uSubSample,vSubSample,width,height);

            //upSampling
            int[][] upSampledYUV = upSample(subSampledYUV,width,height);

            //ConvertToRGB
            int[][] unQuantizedRGB = convertToRGB(upSampledYUV,width,height);

            //QuantizationRange
            int[] quantizedRange = getQuantizedRange(quantizationFactor);

            //QuantizingRGB
            int[][] quantizedRGB = quantizeRGB(unQuantizedRGB, quantizedRange);

            //subSampledImageCreation
            ind = 0;
            for(int y = 0; y < height; y++){
                for(int x = 0; x < width; x++){
                    int pix = 0xff000000 | (quantizedRGB[0][ind] << 16) | (quantizedRGB[1][ind] << 8) | quantizedRGB[2][ind];
                    oldImg.setRGB(x,y,pix);
                    ind++;
                }
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Use labels to display the images
        frame = new JFrame();
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        JLabel lbText1 = new JLabel("Original image");
        lbText1.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel lbText2 = new JLabel("Image before using Uniform Quantization");
        lbText2.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel lbText3 = new JLabel("Image after using Uniform Quantization");
        lbText3.setHorizontalAlignment(SwingConstants.CENTER);
        lbIm1 = new JLabel(new ImageIcon(img));
        lbIm2 = new JLabel(new ImageIcon(oldImg));
        lbIm3 = new JLabel(new ImageIcon(newImg));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;
        frame.getContentPane().add(lbText1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
        frame.getContentPane().add(lbText2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 2;
        c.gridy = 0;
        frame.getContentPane().add(lbText3, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        frame.getContentPane().add(lbIm1, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 1;
        frame.getContentPane().add(lbIm2, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 1;
        frame.getContentPane().add(lbIm3, c);

        frame.pack();
        frame.setVisible(true);
    }

    public int[][] upSample(int[][] subSampledYUV, int width, int height){
        int[][] upSampledYUV = new int[3][subSampledYUV[0].length];
        int ind = 0;
        for(int p = 0; p < height; p++){
            for(int q = 0; q < width; q++) {
                upSampledYUV[0][ind] = subSampledYUV[0][ind]!=Integer.MIN_VALUE ? subSampledYUV[0][ind] : getAvg(subSampledYUV[0],ind);
                upSampledYUV[1][ind] = subSampledYUV[1][ind]!=Integer.MIN_VALUE ? subSampledYUV[1][ind] : getAvg(subSampledYUV[1],ind);
                upSampledYUV[2][ind] = subSampledYUV[2][ind]!=Integer.MIN_VALUE ? subSampledYUV[2][ind] : getAvg(subSampledYUV[2],ind);
                ind++;
            }
        }
        return upSampledYUV;
    }

    public int getAvg(int[] a, int index){
        int previous=Integer.MIN_VALUE,next = Integer.MIN_VALUE;
        for(int i = index; i>=0; --i){
            if(a[i] != Integer.MIN_VALUE){
                previous = a[i];
                break;
            }
        }
        for(int i=index;i<a.length;++i){
            if(a[i] != Integer.MIN_VALUE){
                next = a[i];
                break;
            }
        }
        if(previous!=Integer.MIN_VALUE && next!= Integer.MIN_VALUE){
            return (previous+next)/2;
        }else if(previous != Integer.MIN_VALUE){
            return previous;
        }else{
            return next;
        }
    }

    public int[][] subSample(int[][] givenYUV, int y, int u, int v, int width, int height){
        int[][] subSampledYUV = new int[3][givenYUV[0].length];
        int ind = 0;
        for(int p = 0; p < height; p++){
            for(int q = 0; q < width; q++){
                subSampledYUV[0][ind] = q % y == 0 ? givenYUV[0][ind] : Integer.MIN_VALUE;
                subSampledYUV[1][ind] = q % u == 0 ? givenYUV[1][ind] : Integer.MIN_VALUE;
                subSampledYUV[2][ind] = q % v == 0 ? givenYUV[2][ind] : Integer.MIN_VALUE;
                ind++;
            }
        }
        return subSampledYUV;
    }

    public int[][] convertToYUV(byte[] rgb, int width,int height){
        int[][] yuv = new int[3][rgb.length/3];
        int ind = 0;
        for(int p = 0; p < height; p++){
            for(int q = 0; q < width; q++){
                int r = Byte.toUnsignedInt(rgb[ind]);
                int g = Byte.toUnsignedInt(rgb[ind+height*width]);
                int b = Byte.toUnsignedInt(rgb[ind+height*width*2]);

                int y = Math.round((float)(0.299*r + 0.587*g + 0.114*b));
                int u = Math.round((float)(0.596*r - 0.274*g - 0.322*b));
                int v = Math.round((float)(0.211*r - 0.523*g + 0.312*b));

                yuv[0][ind] = y;
                yuv[1][ind] = u;
                yuv[2][ind] = v;
				/*System.out.println(y+" "+u+" "+v);*/
                ind++;
            }
        }
        return yuv;
    }

    public int[][] convertToRGB(int[][] yuv, int width, int height){
        int[][] rgb = new int[3][yuv[0].length];
        int ind = 0;
        for(int p = 0; p < height; p++){
            for(int q = 0; q < width; q++){
                int y = yuv[0][ind];
                int u = yuv[1][ind];
                int v = yuv[2][ind];

                int r = Math.round((float)(1.000*y + 0.956*u + 0.621*v));
                int g = Math.round((float)(1.000*y - 0.272*u - 0.647*v));
                int b = Math.round((float)(1.000*y - 1.106*u + 1.703*v));

                rgb[0][ind] = r>=0?r:0;
                rgb[1][ind] = g>=0?g:0;
                rgb[2][ind] = b>=0?b:0;
                ind++;
            }
        }
        return rgb;
    }

    int[] getQuantizedRange(int quantizationFactor){
        int[] quantizedRange = new int[quantizationFactor];
        for(int i=0; i<quantizationFactor; ++i){
            quantizedRange[i] = Math.round(i * (float)(256.0/quantizationFactor));
        }
        return quantizedRange;
    }

    int[][] quantizeRGB(int[][] unQuantizedRGB, int[] quantizedRange){
        int[][] quantizedRGB = new int[unQuantizedRGB.length][unQuantizedRGB[0].length];
        for(int i=0; i<3; ++i){
            for(int j=0; j<unQuantizedRGB[0].length; ++j){
                quantizedRGB[i][j] = getClosestFromRange(unQuantizedRGB[i][j],quantizedRange);
            }
        }
        return quantizedRGB;
    }

    //Todo : do binary search for faster results
    int getClosestFromRange(int element, int[] range){
        int closestIndex = 0;
        for(int i=1; i<range.length; ++i){
            if(Math.abs(range[i]-element) < Math.abs(range[closestIndex]-element)){
                closestIndex = i;
            }
        }
        return range[closestIndex];
    }
    void mapAdd(Map<Integer,Integer> map, int a){
        if(map.containsKey(a)){
            map.put(a,map.get(a)+1);
        }else{
            map.put(a,1);
        }
    }

    public static void main(String[] args) {
        Analysis3WithUniformQuantization ren = new Analysis3WithUniformQuantization();
        ren.showIms(args);
    }

}