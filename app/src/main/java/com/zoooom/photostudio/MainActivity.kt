package com.zoooom.photostudio

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import android.graphics.drawable.GradientDrawable

class MainActivity : Activity() {

    private lateinit var photoGrid: GridLayout
    private lateinit var status: TextView
    private lateinit var preview: ImageView
    private lateinit var zoomBar: SeekBar

    private val photoUris = mutableListOf<android.net.Uri>()
    private val maxPhotos = 30

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun button(
        text: String,
        color: Int = 0xFF252B45.toInt()
    ): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(12).toFloat()
            }
            isAllCaps = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(0xFF0B1020.toInt())
        }

        val title = TextView(this).apply {
            text = "ZOOOOM PHOTO STUDIO"
            textSize = 23f
            setTextColor(0xFF70E1FF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Panorama • Ultra Zoom • Photo Studio"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
        }
        root.addView(subtitle)

        status = TextView(this).apply {
            text = "0 / 30 photos selected"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(status)

        val addButton = button(
            "＋ Add Photos (Max 30)",
            0xFF006D8F.toInt()
        )
        root.addView(addButton)
        addButton.setOnClickListener { choosePhotos() }

        val previewTitle = TextView(this).apply {
            text = "PHOTO PREVIEW — Zoom"
            setTextColor(0xFF70E1FF.toInt())
            setPadding(0, dp(12), 0, dp(6))
        }
        root.addView(previewTitle)

        preview = ImageView(this).apply {
            setBackgroundColor(0xFF171D30.toInt())
            scaleType = ImageView.ScaleType.MATRIX
            contentDescription = "Selected photo preview"
        }

        root.addView(
            preview,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(230)
            )
        )

        zoomBar = SeekBar(this).apply {
            max = 300
            progress = 100
        }
        root.addView(zoomBar)

        zoomBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (photoUris.isNotEmpty()) {
                        showPreview(
                            photoUris[0],
                            0.5f + progress / 100f
                        )
                    }
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {}

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {}
            }
        )

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val clearButton = button("Clear")
        val backButton = button("Back")
        val saveButton = button("Save Info")

        actions.addView(
            clearButton,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )
        actions.addView(
            backButton,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )
        actions.addView(
            saveButton,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )

        root.addView(actions)

        clearButton.setOnClickListener {
            photoUris.clear()
            photoGrid.removeAllViews()
            preview.setImageDrawable(null)
            status.text = "0 / 30 photos selected"
            zoomBar.progress = 100
        }

        backButton.setOnClickListener {
            if (photoUris.isNotEmpty()) {
                photoUris.removeAt(photoUris.lastIndex)
                renderGrid()

                if (photoUris.isNotEmpty()) {
                    showPreview(photoUris[0], 1f)
                } else {
                    preview.setImageDrawable(null)
                }
            } else {
                finish()
            }
        }

        saveButton.setOnClickListener {
            Toast.makeText(
                this,
                "Selected photos: ${photoUris.size}",
                Toast.LENGTH_SHORT
            ).show()
        }

        val gridTitle = TextView(this).apply {
            text = "SELECTED PHOTOS"
            setTextColor(Color.WHITE)
            setPadding(0, dp(12), 0, dp(5))
        }
        root.addView(gridTitle)

        val scroll = ScrollView(this)
        photoGrid = GridLayout(this).apply {
            columnCount = 3
            alignmentMode = GridLayout.ALIGN_BOUNDS
        }
        scroll.addView(photoGrid)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    private fun choosePhotos() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        startActivityForResult(intent, 101)
    }

    @Deprecated("Compatible activity result callback")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != 101 || resultCode != Activity.RESULT_OK) {
            return
        }

        if (data == null) return

        val incoming = mutableListOf<android.net.Uri>()

        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                incoming.add(clip.getItemAt(i).uri)
            }
        } ?: data.data?.let {
            incoming.add(it)
        }

        var added = 0

        for (uri in incoming) {
            if (photoUris.size >= maxPhotos) break

            if (!photoUris.contains(uri)) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                photoUris.add(uri)
                added++
            }
        }

        renderGrid()

        if (photoUris.isNotEmpty()) {
            showPreview(photoUris[0], 1f)
        }

        Toast.makeText(
            this,
            "$added photos added",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun renderGrid() {
        photoGrid.removeAllViews()
        status.text = "${photoUris.size} / $maxPhotos photos selected"

        photoUris.forEachIndexed { index, uri ->
            val image = ImageView(this).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFF252B45.toInt())
                contentDescription = "Photo ${index + 1}"

                setOnClickListener {
                    showPreview(uri, 1f)
                    zoomBar.progress = 100
                }
            }

            try {
                contentResolver.openInputStream(uri)?.use {
                    image.setImageBitmap(
                        BitmapFactory.decodeStream(it)
                    )
                }
            } catch (_: Exception) {
                image.setImageDrawable(null)
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(105)
                columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED,
                    1f
                )
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }

            photoGrid.addView(image, params)
        }
    }

    private fun showPreview(
        uri: android.net.Uri,
        zoom: Float
    ) {
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            val maxDimension = 2048
            var sample = 1

            while (
                options.outWidth / sample > maxDimension ||
                options.outHeight / sample > maxDimension
            ) {
                sample *= 2
            }

            val bitmapOptions = BitmapFactory.Options().apply {
                inSampleSize = sample
            }

            val bitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bitmapOptions)
            } ?: return

            preview.setImageBitmap(bitmap)

            preview.post {
                val viewWidth = preview.width.toFloat()
                val viewHeight = preview.height.toFloat()

                val fit = minOf(
                    viewWidth / bitmap.width,
                    viewHeight / bitmap.height
                )

                val scale = fit * zoom.coerceIn(0.5f, 3.5f)

                val matrix = Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(
                        (viewWidth - bitmap.width * scale) / 2f,
                        (viewHeight - bitmap.height * scale) / 2f
                    )
                }

                preview.imageMatrix = matrix
            }
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Photo preview could not be opened",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
