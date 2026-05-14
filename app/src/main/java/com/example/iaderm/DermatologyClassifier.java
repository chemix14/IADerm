package com.example.iaderm;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;

/**
 * DermatologyClassifier — Handles inference for skin disease classification.
 * 
 * Loads the TFLite model from assets and processes captured images to
 * obtain a diagnosis score and probability.
 */
public class DermatologyClassifier {

    private static final String TAG = "DermatologyClassifier";
    private static final String MODEL_FILE = "dermatology_model.tflite";
    
    private Interpreter interpreter;
    private final int inputSize = 224; // MobileViT default
    
    // Pre-processing constants for MobileViT (ImageNet normalization)
    // Formula: (pixel - mean * 255) / (std * 255)
    private static final float IMAGE_MEAN = 123.675f; // 0.485 * 255
    private static final float IMAGE_STD = 58.395f;   // 0.229 * 255

    // Classes sorted alphabetically as in the training script
    private final String[] PADECIMIENTOS = {
        "Acné", "Cloasma", "Dermatitis seborreica", "Eccema", "Eritema", 
        "Hiperpigmentación", "Lentigos", "Manchas solares", "Melasma", 
        "Nevus (lunares)", "Piel sana", "Psoriasis", "Queratosis actínica", 
        "Rosácea", "Vitíligo"
    };

    public static class ClassificationResult {
        public String diagnosis;
        public int score;
        public String top3Data;

        public ClassificationResult(String diagnosis, int score, String top3Data) {
            this.diagnosis = diagnosis;
            this.score = score;
            this.top3Data = top3Data;
        }
    }

    public DermatologyClassifier(Context context) {
        try {
            MappedByteBuffer model = FileUtil.loadMappedFile(context, MODEL_FILE);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            interpreter = new Interpreter(model, options);
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model", e);
        }
    }

    /**
     * Classifies the given bitmap and returns the diagnosis, score and top 3 predictions.
     */
    public ClassificationResult classify(Bitmap bitmap) {
        if (interpreter == null) return new ClassificationResult("Desconocido", 0, "");

        // 1. Pre-process the image
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(inputSize, inputSize, ResizeOp.ResizeMethod.BILINEAR))
                .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                .build();

        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);

        // 2. Prepare output buffer
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32);

        // 3. Run inference
        interpreter.run(tensorImage.getBuffer(), outputBuffer.getBuffer().rewind());

        // 4. Process results
        float[] results = outputBuffer.getFloatArray();
        
        // Apply Softmax if the model outputs logits (values outside [0,1])
        float[] probabilities = softmax(results);
        Log.d(TAG, "Probabilities: " + Arrays.toString(probabilities));

        // Sort indices by probability
        Integer[] indices = new Integer[PADECIMIENTOS.length];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        Arrays.sort(indices, (a, b) -> Float.compare(probabilities[b], probabilities[a]));

        StringBuilder top3Builder = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            int idx = indices[i];
            top3Builder.append(PADECIMIENTOS[idx]).append(":")
                       .append((int) (probabilities[idx] * 100));
            if (i < 2) top3Builder.append(";");
        }

        String diagnosis = PADECIMIENTOS[indices[0]];
        int score = (int) (probabilities[indices[0]] * 100);

        return new ClassificationResult(diagnosis, score, top3Builder.toString());
    }

    private float[] softmax(float[] logits) {
        float[] output = new float[logits.length];
        float maxLogit = Float.NEGATIVE_INFINITY;
        for (float logit : logits) maxLogit = Math.max(maxLogit, logit);
        
        float sum = 0;
        for (int i = 0; i < logits.length; i++) {
            output[i] = (float) Math.exp(logits[i] - maxLogit);
            sum += output[i];
        }
        for (int i = 0; i < output.length; i++) output[i] /= sum;
        return output;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
