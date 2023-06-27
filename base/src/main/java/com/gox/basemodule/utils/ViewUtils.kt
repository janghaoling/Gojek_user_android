package com.gox.basemodule.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast

import com.bumptech.glide.Glide
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.gox.basemodule.BuildConfig
import com.gox.basemodule.R

import java.io.ByteArrayOutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Objects

import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

import es.dmoral.toasty.Toasty
import permissions.dispatcher.PermissionRequest

object ViewUtils {

    @MainThread
    fun showToast(context: Context, @StringRes messageResId: Int, isSuccess: Boolean) {
        if (isSuccess)
            Toasty.success(context, messageResId, Toast.LENGTH_SHORT).show()
        else
            Toasty.error(context, messageResId, Toast.LENGTH_SHORT).show()
    }

    @MainThread
    fun showToast(context: Context, messageResId: String?, isSuccess: Boolean) {
        if (isSuccess)
            Toasty.success(context, messageResId!!, Toast.LENGTH_SHORT).show()
        else
            Toasty.error(context, messageResId!!, Toast.LENGTH_SHORT).show()
    }

    @MainThread
    fun showNormalToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @MainThread
    fun showTransportAlert(context: Context, @StringRes messageResId: Int, callBack: ViewCallBack.Alert) {
        AlertDialog.Builder(context,R.style.TransportThemeDialog)
                .setTitle(R.string.app_name)
                .setMessage(messageResId)
                .setPositiveButton(android.R.string.yes) { dialog, which -> callBack.onPositiveButtonClick(dialog) }
                .setNegativeButton(android.R.string.no) { dialog, which -> callBack.onNegativeButtonClick(dialog) }
                .show()
    }
    fun showAlert(context: Context, @StringRes messageResId: Int, callBack: ViewCallBack.Alert) {
        AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(messageResId)
                .setPositiveButton(android.R.string.yes) { dialog, which -> callBack.onPositiveButtonClick(dialog) }
                .setNegativeButton(android.R.string.no) { dialog, which -> callBack.onNegativeButtonClick(dialog) }
                .show()
    }

    @MainThread
    fun showAlert(context: Context, @StringRes messageResId: Int) {
        AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(messageResId)
                .setPositiveButton(android.R.string.yes) { dialog, which -> dialog.dismiss() }
                .show()
    }

    @MainThread
    fun showRationaleAlert(context: Context, @StringRes messageResId: Int, request: PermissionRequest) {
        AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(messageResId)
                .setPositiveButton(context.getString(R.string.allow)) { dialog, which -> request.proceed() }
                .setNegativeButton(context.getString(R.string.deny)) { dialog, which -> request.cancel() }
                .show()
    }

    fun bitmapDescriptorFromVector(context: Context, @DrawableRes vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(0, 0, Objects.requireNonNull(vectorDrawable).intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    fun convertToBitmap(drawable: Drawable, widthPixels: Int, heightPixels: Int): Bitmap {
        val mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mutableBitmap)
        drawable.setBounds(0, 0, widthPixels, heightPixels)
        drawable.draw(canvas)

        return mutableBitmap
    }

    fun getTimeDifference(date: String): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-mm-dd hh:mm:ss")

        try {
            val date1 = simpleDateFormat.parse(simpleDateFormat.format(Calendar.getInstance().time))
            val date2 = simpleDateFormat.parse(date)

            var different = date2.time - date1.time
            val secondsInMilli: Long = 1000
            val minutesInMilli = secondsInMilli * 60
            val hoursInMilli = minutesInMilli * 60
            val daysInMilli = hoursInMilli * 24

            val elapsedDays = different / daysInMilli
            different = different % daysInMilli

            val elapsedHours = different / hoursInMilli
            different = different % hoursInMilli

            val elapsedMinutes = different / minutesInMilli
            different = different % minutesInMilli

            val elapsedSeconds = different / secondsInMilli

            return if (elapsedHours == 0L)
                if (elapsedMinutes > 1) "$elapsedMinutes mins" else elapsedMinutes.toString() + "min"
            else
                if (elapsedHours > 1) "$elapsedHours hrs" else elapsedMinutes.toString() + "hr"

        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return "0"
    }

    fun setImageViewGlide(context: Context, imageView: ImageView, imagePath: String) {

        Glide.with(context)
                .load(imagePath)
                .thumbnail(0.5f)
                .error(R.drawable.image_placeholder)
                .placeholder(R.drawable.image_placeholder)
                .into(imageView)

    }


}
