@file:JvmName("RPickMedia")

package pyxis.uzuki.live.richutilskt.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import pyxis.uzuki.live.richutilskt.impl.F2
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class RPickMedia private constructor() {
    private var IMAGE_CONTENT_URL = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    private var VIDEO_CONTENT_URL = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    private var mCurrentPhotoURL: String? = null
    private var mCurrentVideoURL: String? = null
    private val PERMISSION_ARRAY = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)

    private fun getActivity(context: Context): Activity? {
        var c = context

        while (c is ContextWrapper) {
            if (c is Activity) {
                return c
            }
            c = c.baseContext
        }
        return null
    }

    private fun Context.requestPermission(listener: (Boolean) -> Unit) {
        RPermission.instance.checkPermission(this, PERMISSION_ARRAY, { code, _ ->
            listener.invoke(code == RPermission.PERMISSION_GRANTED)
        })
    }

    /**
     * enable internal storage mode
     *
     * @param [isInternal] capture Image/Video in internal storage
     */
    fun setInternalStorage(isInternal: Boolean) {
        if (isInternal) {
            IMAGE_CONTENT_URL = MediaStore.Images.Media.INTERNAL_CONTENT_URI
            VIDEO_CONTENT_URL = MediaStore.Video.Media.INTERNAL_CONTENT_URI
        } else {
            IMAGE_CONTENT_URL = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            VIDEO_CONTENT_URL = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    }

    /**
     * pick image from Camera
     *
     * @param[callback] callback
     */
    fun pickFromCamera(context: Context, callback: (Int, String) -> Unit) {
        context.requestPermission {
            if (it) {
                requestPhotoPick(context, PICK_FROM_CAMERA, callback)
            } else {
                callback.invoke(PICK_FAILED, "")
            }
        }
    }

    /**
     * pick image from Camera
     *
     * @param[callback] callback
     */
    fun pickFromCamera(context: Context, callback: F2<Int, String>?) {
        context.requestPermission {
            if (it) {
                requestPhotoPick(context, PICK_FROM_CAMERA, { code, uri -> callback?.invoke(code, uri) })
            } else {
                callback?.invoke(PICK_FAILED, "")
            }
        }
    }

    /**
     * pick image from Gallery
     *
     * @param[callback] callback
     */
    fun pickFromGallery(context: Context, callback: (Int, String) -> Unit) {
        context.requestPermission {
            if (it) {
                requestPhotoPick(context, PICK_FROM_GALLERY, callback)
            } else {
                callback.invoke(PICK_FAILED, "")
            }
        }
    }

    /**
     * pick image from Gallery
     *
     * @param[callback] callback
     */
    fun pickFromGallery(context: Context, callback: F2<Int, String>?) {
        context.requestPermission {
            if (it) {
                requestPhotoPick(context, PICK_FROM_GALLERY, { code, uri -> callback?.invoke(code, uri) })
            } else {
                callback?.invoke(PICK_FAILED, "")
            }
        }
    }

    /**
     * pick image from Video
     *
     * @param[callback] callback
     */
    fun pickFromVideo(context: Context, callback: (Int, String) -> Unit) {
        context.requestPermission {
            if (it) {
                requestPhotoPick(context, PICK_FROM_VIDEO, callback)
            } else {
                callback.invoke(PICK_FAILED, "")
            }
        }
    }

    /**
     * pick image from Video
     *
     * @param[callback] callback
     */
    fun pickFromVideo(context: Context, callback: F2<Int, String>?) {
        context.requestPermission {
            if (it) {
                requestPhotoPick(context, PICK_FROM_VIDEO, { code, uri -> callback?.invoke(code, uri) })
            } else {
                callback?.invoke(PICK_FAILED, "")
            }
        }
    }

    /**
     * pick image from Camera (Video Mode)
     *
     * @param[callback] callback
     */
    fun pickFromVideoCamera(context: Context, callback: (Int, String) -> Unit) {
        context.requestPermission {
            if (it) {
                requestPhotoPick(context, PICK_FROM_CAMERA_VIDEO, callback)
            } else {
                callback.invoke(PICK_FAILED, "")
            }
        }
    }

    /**
     * pick image from Camera (Video Mode)
     *
     * @param[callback] callback
     */
    fun pickFromVideoCamera(context: Context, callback: F2<Int, String>?) {
        context.requestPermission {
            if (it) {
                requestPhotoPick(context, PICK_FROM_CAMERA_VIDEO, { code, uri -> callback?.invoke(code, uri) })
            } else {
                callback?.invoke(PICK_FAILED, "")
            }
        }

    }

    @SuppressLint("ValidFragment")
    private fun requestPhotoPick(context: Context, pickType: Int, callback: (Int, String) -> Unit) {

        val fragmentManager = getActivity(context)?.fragmentManager
        val fragment = ResultFragment(fragmentManager as FragmentManager)

        fragmentManager.beginTransaction().add(fragment, "FRAGMENT_TAG").commitAllowingStateLoss()
        fragmentManager.executePendingTransactions()

        val intent = Intent()

        when (pickType) {
            PICK_FROM_CAMERA -> {
                intent.action = MediaStore.ACTION_IMAGE_CAPTURE
                val captureUri = createImageUri(context)
                mCurrentPhotoURL = captureUri.toString()
                intent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
            }

            PICK_FROM_GALLERY -> {
                intent.action = Intent.ACTION_PICK
                intent.type = android.provider.MediaStore.Images.Media.CONTENT_TYPE
            }

            PICK_FROM_VIDEO -> {
                intent.action = Intent.ACTION_PICK
                intent.type = android.provider.MediaStore.Video.Media.CONTENT_TYPE
            }

            PICK_FROM_CAMERA_VIDEO -> {
                intent.action = MediaStore.ACTION_VIDEO_CAPTURE
                val captureUri = createVideoUri(context)
                mCurrentVideoURL = captureUri.toString()
                intent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
            }
        }

        fragment.setContextReference(context)
        fragment.setCallback(callback)
        fragment.setArguments(mCurrentPhotoURL, mCurrentPhotoURL, pickType)
        fragment.startActivityForResult(intent, pickType)
    }

    private fun createImageUri(context: Context): Uri {
        val contentResolver = context.contentResolver
        val cv = ContentValues()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        cv.put(MediaStore.Images.Media.TITLE, timeStamp)
        return contentResolver.insert(IMAGE_CONTENT_URL, cv)
    }

    private fun createVideoUri(context: Context): Uri {
        val contentResolver = context.contentResolver
        val cv = ContentValues()
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        cv.put(MediaStore.Images.Media.TITLE, timeStamp)
        return contentResolver.insert(VIDEO_CONTENT_URL, cv)
    }

    @SuppressLint("ValidFragment")
    class ResultFragment() : Fragment() {
        private var mContext: WeakReference<Context> = WeakReference<Context>(null)
        private var mFragmentManager: FragmentManager? = null
        private lateinit var mCallback: ((Int, String) -> Unit)
        private var mCurrentPhotoURL = ""
        private var mCurrentVideoURL = ""
        private var mRequestCode = 0

        constructor(fm: FragmentManager) : this() {
            this.mFragmentManager = fm
        }

        fun setArguments(currentPhotoPath: String?, currentVideoPath: String?, requestCode: Int) {
            this.mCurrentPhotoURL = currentPhotoPath ?: ""
            this.mCurrentVideoURL = currentVideoPath ?: ""
            this.mRequestCode = requestCode
        }

        fun setCallback(callback: (Int, String) -> Unit) {
            mCallback = callback
        }

        fun setContextReference(context: Context) {
            mContext = WeakReference(context)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            when (requestCode) {
                PICK_FROM_CAMERA ->
                    if (resultCode == Activity.RESULT_OK) {
                        mCurrentPhotoURL.let { mCallback.invoke(PICK_SUCCESS, Uri.parse(it) getRealPath (activity)) }
                    } else {
                        mCallback.invoke(PICK_FAILED, "")
                    }

                PICK_FROM_GALLERY ->
                    if (resultCode == Activity.RESULT_OK) {
                        mCallback.invoke(PICK_SUCCESS, data?.data?.getRealPath((activity)) as String)
                    } else {
                        mCallback.invoke(PICK_FAILED, "")
                    }

                PICK_FROM_VIDEO ->
                    if (resultCode == Activity.RESULT_OK) {
                        mCallback.invoke(PICK_SUCCESS, data?.data?.getRealPath((activity)) as String)
                    } else {
                        mCallback.invoke(PICK_FAILED, "")
                    }

                PICK_FROM_CAMERA_VIDEO ->
                    if (resultCode == Activity.RESULT_OK) {
                        var path = data?.data?.getRealPath(activity) as String
                        if (path.isEmpty()) {
                            path = mCurrentVideoURL
                        }

                        path.let {
                            mCallback.invoke(PICK_SUCCESS, path)
                        }
                    } else {
                        mCallback.invoke(PICK_FAILED, "")
                    }
            }

            mFragmentManager?.beginTransaction()?.remove(this)?.commit()
        }

        private fun verifyPermissions(grantResults: IntArray): Boolean =
                if (grantResults.isEmpty()) false else grantResults.none { it != PackageManager.PERMISSION_GRANTED }
    }

    companion object {
        @JvmField
        var instance: RPickMedia = RPickMedia()

        val PICK_FROM_CAMERA = 0
        val PICK_FROM_GALLERY = 1
        val PICK_FROM_VIDEO = 2
        val PICK_FROM_CAMERA_VIDEO = 3

        @JvmField
        val PICK_SUCCESS = 1
        @JvmField
        val PICK_FAILED = 0
    }

}