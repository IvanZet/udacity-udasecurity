package com.udacity.image.service;

import java.awt.image.BufferedImage;

/**
 * Interface showing the methods our image service will need to support
 */
public interface ImageService {
    boolean imageContainsCat(BufferedImage image, float confidenceThreshold);
}
