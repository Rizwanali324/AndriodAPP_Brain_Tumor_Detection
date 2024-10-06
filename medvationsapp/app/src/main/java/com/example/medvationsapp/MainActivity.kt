package com.example.medvationsapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var selectBtn: Button
    private lateinit var predBtn: Button
    private lateinit var resView: TextView
    private lateinit var imageView: ImageView
    private lateinit var bitmap: Bitmap

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var detector: Detector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selectBtn = findViewById(R.id.selectBtn)
        predBtn = findViewById(R.id.predictBtn)
        resView = findViewById(R.id.resView)
        imageView = findViewById(R.id.imageView)

        // Initialize the Detector
        detector = Detector(
            context = this,
            modelPath = "best_float32.tflite",
            labelPath = "labels.txt",
            detectorListener = this
        )
        detector.setup()

        // Initialize the result launcher for selecting an image
        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val imageUri = result.data?.data
                try {
                    // Reset the imageView and resView before displaying the new image
                    imageView.setImageResource(0) // Clears previous image
                    resView.text = ""              // Clears previous prediction result

                    bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                    imageView.setImageBitmap(bitmap)

                    // Reset the TextView to indicate no predictions made yet
                    resView.text = "Image selected. Click 'Predict' to detect objects."
                } catch (e: Exception) {
                    e.printStackTrace()
                    resView.text = "Failed to load image."
                }
            }
        }

        // Set up the button listeners
        selectBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            resultLauncher.launch(intent)
        }

        predBtn.setOnClickListener {
            if (::bitmap.isInitialized) {
                detector.detect(bitmap)
            } else {
                resView.text = "No image selected."
            }
        }
    }

    override fun onEmptyDetect() {
        resView.text = "No objects detected."
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>) {
        val canvasBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(canvasBitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.RED
            strokeWidth = 5f
            style = Paint.Style.STROKE
        }
        val textPaint = Paint().apply {
            color = android.graphics.Color.YELLOW
            textSize = 15f
            style = Paint.Style.FILL
        }

        // List to collect the names of detected objects
        val detectedObjectNames = mutableListOf<String>()

        for (box in boundingBoxes) {
            val rectF = RectF(
                box.x1 * bitmap.width,
                box.y1 * bitmap.height,
                box.x2 * bitmap.width,
                box.y2 * bitmap.height
            )
            canvas.drawRect(rectF, paint)
            val formattedScore = String.format("%.2f", box.cnf * 100) // Format score to 2 decimal places
            val labelText = "${box.clsName} ($formattedScore%)"
            canvas.drawText(labelText, rectF.left, rectF.top - 10, textPaint)

            // Add the detected object name to the list
            detectedObjectNames.add("${box.clsName} ($formattedScore%)")
        }

        imageView.setImageBitmap(canvasBitmap)

        // Update the TextView to display the detected object names
        if (detectedObjectNames.isNotEmpty()) {
            resView.text = "Detected: ${detectedObjectNames.joinToString(", ")}"
        } else {
            resView.text = "No objects detected."
        }
    }
}
