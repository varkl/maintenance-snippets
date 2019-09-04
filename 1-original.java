public void write(Image image, Class<? extends Texture> textureType, String fileName) {

    FileOutputStream outs = null;
    try {
        File file = new File(filePath + "/" + fileName);
        outs = new FileOutputStream(file);

        DataOutput out = new DataOutputStream(outs);

        //fileID
        out.write(fileIdentifier);
        //endianness
        out.writeInt(0x04030201);
        GLImageFormat format = getGlFormat(image.getFormat());
        //glType
        out.writeInt(format.dataType);
        //glTypeSize
        out.writeInt(1);
        //glFormat
        out.writeInt(format.format);
        //glInernalFormat
        out.writeInt(format.internalFormat);
        //glBaseInternalFormat
        out.writeInt(format.format);
        //pixelWidth
        out.writeInt(image.getWidth());
        //pixelHeight
        out.writeInt(image.getHeight());

        int pixelDepth = 1;
        int numberOfArrayElements = 1;
        int numberOfFaces = 1;
        if (image.getDepth() > 1) {
            //pixelDepth
            if (textureType == Texture3D.class) {
                pixelDepth = image.getDepth();
            }
        }
        if(image.getData().size()>1){
            //numberOfArrayElements
            if (textureType == TextureArray.class) {
                numberOfArrayElements = image.getData().size();
            }
            //numberOfFaces
            if (textureType == TextureCubeMap.class) {
                numberOfFaces = image.getData().size();
            }
        }
        out.writeInt(pixelDepth);
        out.writeInt(numberOfArrayElements);
        out.writeInt(numberOfFaces);

        int numberOfMipmapLevels = 1;
        //numberOfMipmapLevels
        if (image.hasMipmaps()) {
            numberOfMipmapLevels = image.getMipMapSizes().length;
        }
        out.writeInt(numberOfMipmapLevels);

        //bytesOfKeyValueData
        String keyValues = "KTXorientation\0S=r,T=u\0";
        int bytesOfKeyValueData = keyValues.length() + 4;
        int padding = 3 - ((bytesOfKeyValueData + 3) % 4);
        bytesOfKeyValueData += padding;
        out.writeInt(bytesOfKeyValueData);

        //keyAndValueByteSize
        out.writeInt(bytesOfKeyValueData - 4 - padding);
        //values
        out.writeBytes(keyValues);
        pad(padding, out);

        int offset = 0;
        //iterate over data
        for (int mipLevel = 0; mipLevel < numberOfMipmapLevels; mipLevel++) {

            int width = Math.max(1, image.getWidth() >> mipLevel);
            int height = Math.max(1, image.getHeight() >> mipLevel);

            int imageSize;

            if (image.hasMipmaps()) {
                imageSize = image.getMipMapSizes()[mipLevel];
            } else {
                imageSize = width * height * image.getFormat().getBitsPerPixel() / 8;
            }
            out.writeInt(imageSize);

            for (int arrayElem = 0; arrayElem < numberOfArrayElements; arrayElem++) {
                for (int face = 0; face < numberOfFaces; face++) {
                    int nbPixelWritten = 0;
                    for (int depth = 0; depth < pixelDepth; depth++) {
                        ByteBuffer byteBuffer = image.getData(getSlice(face, arrayElem));
                        // BufferUtils.ensureLargeEnough(byteBuffer, imageSize);
                        log.log(Level.FINE, "position {0}", byteBuffer.position());
                        byteBuffer.position(offset);
                        byte[] b = getByteBufferArray(byteBuffer, imageSize);
                        out.write(b);

                        nbPixelWritten = b.length;
                    }
                    //cube padding
                    if (numberOfFaces == 6 && numberOfArrayElements == 0) {
                        padding = 3 - ((nbPixelWritten + 3) % 4);
                        pad(padding, out);
                    }
                }
            }
            //mip padding
            log.log(Level.FINE, "skipping {0}", (3 - ((imageSize + 3) % 4)));
            padding = 3 - ((imageSize + 3) % 4);
            pad(padding, out);
            offset += imageSize;
        }

    } catch (FileNotFoundException ex) {
        Logger.getLogger(KTXWriter.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
        Logger.getLogger(KTXWriter.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
        try {
            if(outs != null){
                outs.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(KTXWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
