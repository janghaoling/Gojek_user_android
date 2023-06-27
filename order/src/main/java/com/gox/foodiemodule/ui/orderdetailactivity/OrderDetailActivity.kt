package com.gox.foodiemodule.ui.orderdetailactivity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gox.basemodule.BaseApplication
import com.gox.basemodule.BuildConfig
import com.gox.basemodule.base.BaseActivity
import com.gox.basemodule.common.chatmessage.ChatActivity
import com.gox.basemodule.data.Constant
import com.gox.basemodule.data.PreferenceHelper
import com.gox.basemodule.data.PreferenceKey
import com.gox.basemodule.data.setValue
import com.gox.basemodule.socket.SocketManager
import com.gox.basemodule.utils.ViewUtils
import com.gox.foodiemodule.R
import com.gox.foodiemodule.adapter.OrderItemListAdapter
import com.gox.foodiemodule.data.model.OrderTrackModel
import com.gox.foodiemodule.data.model.ResOrderDetail
import com.gox.foodiemodule.databinding.ActivityOrderpageLayoutBinding
import com.gox.foodiemodule.fragment.coupon.rating.RatingFragment
import com.gox.foodiemodule.fragment.ordercancelreason.OrderReasonFragment
import com.gox.foodiemodule.ui.ordertracking.OrderTrackingActivity
import io.socket.emitter.Emitter
import java.util.*

class OrderDetailActivity : BaseActivity<ActivityOrderpageLayoutBinding>(), OrderDetailNavigator {

    lateinit var mBinding: ActivityOrderpageLayoutBinding
    lateinit var mViewModel: OrderDetailViewModel
    private var checkRequestTimer: Timer? = null
    private var mIshown: Boolean = true
    private var userId: Int? = null
    private var providerId: Int? = null
    private var userName: String? = null
    private var providerName: String? = null
    private var mRequestId: Int? = null
    private val preference = PreferenceHelper(BaseApplication.baseApplication)
    private var otpVerifyFlag: String? = null
    private var orderId:Int = 0
    private var orderStatus:String =""
    private var reqID: Int = 0
    private var mRoomConnected: Boolean = false


    override fun getLayoutId(): Int = R.layout.activity_orderpage_layout

    override fun initView(mViewDataBinding: ViewDataBinding?) {

        this.mBinding = mViewDataBinding as ActivityOrderpageLayoutBinding
        mViewDataBinding.toolbarBackImg.setOnClickListener { finish() }
        mViewModel = ViewModelProviders.of(this).get(OrderDetailViewModel::class.java)
        mViewModel.navigator = this
        checkRequestTimer = Timer()
        orderId = intent.getIntExtra("orderId", 0)
        //      baseLiveDataLoading = mViewModel.loadingProgress
        PreferenceHelper(BaseApplication.baseApplication).setValue(PreferenceKey.ORDER_REQ_ID, orderId)
        reqID = orderId


        SocketManager.onEvent(Constant.ROOM_NAME.STATUS, Emitter.Listener {
            if(it[0].toString().contains(Constant.ModuleTypes.ORDER)){
                mRoomConnected = true
            }
        })


        SocketManager.onEvent(Constant.ROOM_NAME.ORDER_REQ, Emitter.Listener {
            // checkRequestTimer!!.cancel()
            mViewModel.getOrderDetails(orderId)
        })


        if (BuildConfig.ENABLE_TIMER_CALL) {
            checkRequestTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    if(!mRoomConnected) {
                        if (reqID != 0) {
                            SocketManager.emit(Constant.ROOM_NAME.ORDER_ROOM_NAME, Constant.ROOM_ID.getOrderRoom(reqID))
                        }
                    }

                    mViewModel.getOrderDetails(orderId)
                }
            }, 0, 5000)
        } else {
            mViewModel.getOrderDetails(orderId)
        }

        mViewModel.resOrderDetail.observe(this, Observer {
            if (it != null) if (it.statusCode == "200") {
                updateCartDetail(it)
            }
        })

        otpVerifyFlag = BaseApplication.getCustomPreference!!.getString(PreferenceKey.ORDER_OTP, "")!!
    }

    override fun onResume() {
        super.onResume()
        mViewModel.getOrderDetails(orderId)
    }

    override fun onDestroy() {
        super.onDestroy()
        checkRequestTimer?.cancel()
    }

    @SuppressLint("SetTextI18n")
    private fun updateCartDetail(responseData: ResOrderDetail) {
        setOnClickListeners(responseData)
        orderStatus = responseData.responseData?.status?:""

        if (responseData.responseData?.order_type.equals("TAKEAWAY")) {
            stateWhenTakeAway(responseData)

            if (orderStatus.equals("ORDERED")) {
                stateWhenTakeAwayOrdered()

            }

            if (orderStatus.equals("RECEIVED") && responseData.responseData?.order_ready_status == 0) {
                stateWhenTakeAwayReceived()
            }
            if (orderStatus.equals("RECEIVED") && responseData.responseData?.order_ready_status == 1) {
                stateWhenTakeAwayReady()

            }

            if (orderStatus.equals("COMPLETED")) {
                stateWhenTakeAwayCompleted()
            }
        }
        //After Deliery boy accepted req from Restuarent
        else {
            mBinding.takeawayCardView.visibility = View.GONE
            if (orderStatus.equals("RECEIVED")) {
                stateWhenStoreConfirmed()
            }
            if (orderStatus.equals("ORDERED")) {
                stateWhenStoreNotConfirmed()

            } else if (orderStatus.equals("STORECANCELLED")) {

                stateWhenStoreCancelled()

            } else if (orderStatus.equals("SEARCHING")) {


                stateWhenHomeSearching()

            } else if (orderStatus.equals("STARTED")) {

                stateWhenHomeStarted()


            }
            if (orderStatus.equals("PROCESSING")) {


                stateWhenHomeProcessing()


            }
            //Reached to Restuarent
            if (orderStatus.equals("REACHED")) {
                stateWhenHomeReached()

            }

            //Picked Up
            if (orderStatus.equals("PICKEDUP")) {


                stateWhenHomePickedUp()

            }
            //Arriver to our Location
            if (orderStatus.equals("ARRIVED")) {


                stateWhenHomeArrived()

            }
            //Arriver to our Location
            if (orderStatus.equals("COMPLETED")) {
                stateWhenHomeCompleted()
            }
        }

    }

    private fun stateWhenHomeCompleted() {
        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderOntheWayRl.visibility = View.GONE
        mBinding.orderDeliveryRl.visibility = View.VISIBLE
        mBinding.deliveryCardView.visibility = View.VISIBLE
        mBinding.unOrderDeliveryRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.VISIBLE
        mBinding.unOrderOntheWayRl.visibility = View.VISIBLE
        mBinding.mapTrack.visibility = View.VISIBLE
        mBinding.deliveryPersonDetailsLabel.visibility = View.VISIBLE
        if (mIshown) {
            mIshown = false
            val mRatingFragment = RatingFragment.newInstance()
            mRatingFragment.isCancelable = true
            mRatingFragment.show(supportFragmentManager, mRatingFragment.tag)
        }
    }

    private fun stateWhenHomeArrived() {
        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.VISIBLE

        mBinding.orderAcceptedLayoutRl.visibility = View.GONE
        mBinding.orderUnacceptedLayoutRl.visibility = View.VISIBLE

        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.GONE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.VISIBLE

        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.VISIBLE


        mBinding.orderOntheWayRl.visibility = View.GONE
        mBinding.unOrderOntheWayRl.visibility = View.VISIBLE


        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.GONE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.VISIBLE

        mBinding.orderDeliveryRl.visibility = View.VISIBLE
        mBinding.unOrderDeliveryRl.visibility = View.GONE


        mBinding.deliveryCardView.visibility = View.VISIBLE
        mBinding.mapTrack.visibility = View.VISIBLE
        mBinding.deliveryPersonDetailsLabel.visibility = View.VISIBLE
        mBinding.otpLabel.visibility = View.VISIBLE
        showHideOTP()
        mBinding.cancelOrder.visibility = View.GONE
    }

    private fun stateWhenHomePickedUp() {
        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.VISIBLE

        mBinding.orderAcceptedLayoutRl.visibility = View.GONE
        mBinding.orderUnacceptedLayoutRl.visibility = View.VISIBLE

        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.GONE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.VISIBLE

        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.VISIBLE


        mBinding.orderOntheWayRl.visibility = View.VISIBLE
        mBinding.unOrderOntheWayRl.visibility = View.GONE



        mBinding.deliveryCardView.visibility = View.VISIBLE
        mBinding.mapTrack.visibility = View.VISIBLE
        mBinding.deliveryPersonDetailsLabel.visibility = View.VISIBLE
        mBinding.otpLabel.visibility = View.VISIBLE
        showHideOTP()
        mBinding.cancelOrder.visibility = View.GONE
    }

    private fun stateWhenHomeReached() {
        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.VISIBLE

        mBinding.orderAcceptedLayoutRl.visibility = View.GONE
        mBinding.orderUnacceptedLayoutRl.visibility = View.VISIBLE

        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.GONE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.VISIBLE

        mBinding.orderProcessLayoutRl.visibility = View.VISIBLE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.GONE


        mBinding.orderProcssedDescbTv.text = getString(R.string.YourOrdersArePreparinginTheShop)
        mBinding.deliveryCardView.visibility = View.VISIBLE
        mBinding.mapTrack.visibility = View.VISIBLE
        mBinding.deliveryPersonDetailsLabel.visibility = View.VISIBLE
        mBinding.otpLabel.visibility = View.VISIBLE
        showHideOTP()
        mBinding.cancelOrder.visibility = View.GONE
    }

    private fun stateWhenHomeProcessing() {
        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.VISIBLE

        mBinding.orderAcceptedLayoutRl.visibility = View.GONE
        mBinding.orderUnacceptedLayoutRl.visibility = View.VISIBLE

        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.VISIBLE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.GONE

        mBinding.deliveryCardView.visibility = View.VISIBLE
        mBinding.mapTrack.visibility = View.VISIBLE
        mBinding.deliveryPersonDetailsLabel.visibility = View.VISIBLE
        mBinding.otpLabel.visibility = View.VISIBLE
        showHideOTP()
        mBinding.cancelOrder.visibility = View.GONE
    }

    private fun stateWhenHomeStarted() {
        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.VISIBLE

        mBinding.orderAcceptedLayoutRl.visibility = View.GONE
        mBinding.orderUnacceptedLayoutRl.visibility = View.VISIBLE

        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.VISIBLE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.GONE

        mBinding.deliveryCardView.visibility = View.VISIBLE
        mBinding.mapTrack.visibility = View.VISIBLE
        mBinding.deliveryPersonDetailsLabel.visibility = View.VISIBLE
        mBinding.otpLabel.visibility = View.VISIBLE
        showHideOTP()
        mBinding.cancelOrder.visibility = View.GONE
    }

    private fun stateWhenHomeSearching() {
        mBinding.orderAcceptedLayoutRl.visibility = View.VISIBLE
        mBinding.orderUnacceptedLayoutRl.visibility = View.GONE
        mBinding.deliveryCardView.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE
        mBinding.mapTrack.visibility = View.GONE
        mBinding.orderAcceptedDescbTv.text = getText(R.string.order_accepted)
        mBinding.orderAcceptedTv.text = getText(R.string.order_accepted_shop)
        mBinding.otpLabel.visibility = View.VISIBLE
        showHideOTP()
        mBinding.cancelOrder.visibility = View.GONE
    }

    private fun stateWhenStoreCancelled() {
        mBinding.orderAcceptedLayoutRl.visibility = View.VISIBLE
        mBinding.orderUnacceptedLayoutRl.visibility = View.GONE
        mBinding.deliveryCardView.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE
        mBinding.mapTrack.visibility = View.GONE
        mBinding.cancelOrder.visibility = View.VISIBLE
        mBinding.orderAcceptedDescbTv.text = getText(R.string.restaurant_confirmation)
        mBinding.orderAcceptedTv.text = getText(R.string.restaurant_confirmation)
    }

    private fun stateWhenStoreNotConfirmed() {
        mBinding.orderAcceptedLayoutRl.visibility = View.VISIBLE
        mBinding.orderUnacceptedLayoutRl.visibility = View.GONE
        mBinding.deliveryCardView.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE
        mBinding.otpLabel.visibility = View.GONE
        mBinding.otpValueTv.visibility = View.GONE
        mBinding.cancelOrder.visibility = View.VISIBLE
        mBinding.mapTrack.visibility = View.GONE
        mBinding.orderAcceptedDescbTv.text = getText(R.string.restaurant_confirmation)
        mBinding.orderAcceptedTv.text = getText(R.string.restaurant_confirmation)
    }


    private fun stateWhenStoreConfirmed() {
        mBinding.orderAcceptedLayoutRl.visibility = View.VISIBLE
        mBinding.orderUnacceptedLayoutRl.visibility = View.GONE
        mBinding.deliveryCardView.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE
        mBinding.otpLabel.visibility = View.GONE
        mBinding.otpValueTv.visibility = View.GONE
        mBinding.cancelOrder.visibility = View.GONE
        mBinding.mapTrack.visibility = View.GONE
        mBinding.orderAcceptedDescbTv.text = getText(R.string.order_accepted)
        mBinding.orderAcceptedTv.text = getText(R.string.order_accepted_shop)
    }

    private fun stateWhenTakeAwayCompleted() {
        mBinding.orderAcceptedLayoutRl.visibility = View.GONE
        mBinding.orderUnacceptedLayoutRl.visibility = View.VISIBLE
        mBinding.unOrderTakeAwayDeliveryRl.visibility = View.GONE
        mBinding.orderDeliveryTakeAwayHolder.visibility = View.VISIBLE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.GONE
        mBinding.orderPrepareText.text = getText(R.string.order_ready)
        mBinding.orderPrepareDescText.text = getText(R.string.order_ready_for_pickup)

        mBinding.deliveryCardView.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE

        mBinding.mapTrack.visibility = View.GONE

        mBinding.orderProcessLayoutRl.visibility = View.GONE

        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.GONE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.GONE

        mBinding.mapTrack.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE

        mBinding.orderOntheWayRl.visibility = View.GONE
        mBinding.unOrderOntheWayRl.visibility = View.GONE
        mBinding.unOrderDeliveryRl.visibility = View.GONE
        // finish()
        if (mIshown) {
            mIshown = false
            val mRatingFragment = RatingFragment.newInstance()
            mRatingFragment.isCancelable = true
            mRatingFragment.show(supportFragmentManager, mRatingFragment.tag)
        }
    }

    private fun stateWhenTakeAwayReady() {
        mBinding.orderAcceptedLayoutRl.visibility = View.GONE
        mBinding.orderUnacceptedLayoutRl.visibility = View.VISIBLE
        mBinding.unOrderTakeAwayDeliveryRl.visibility = View.GONE
        mBinding.orderDeliveryTakeAwayHolder.visibility = View.VISIBLE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.GONE
        mBinding.orderPrepareText.text = getText(R.string.order_ready)
        mBinding.orderPrepareDescText.text = getText(R.string.order_ready_for_pickup)

        mBinding.deliveryCardView.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE

        mBinding.mapTrack.visibility = View.GONE

        mBinding.orderProcessLayoutRl.visibility = View.GONE

        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.GONE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.GONE

        mBinding.mapTrack.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE

        mBinding.orderOntheWayRl.visibility = View.GONE
        mBinding.unOrderOntheWayRl.visibility = View.GONE
        mBinding.unOrderDeliveryRl.visibility = View.GONE
    }

    private fun stateWhenTakeAwayReceived() {
        mBinding.orderAcceptedLayoutRl.visibility = View.GONE
        mBinding.orderAcceptedLayoutRl.visibility = View.VISIBLE
        mBinding.unOrderTakeAwayDeliveryRl.visibility = View.VISIBLE
        mBinding.orderDeliveryTakeAwayHolder.visibility = View.GONE
        mBinding.orderAcceptedTv.text = getText(R.string.order_status)
        mBinding.orderAcceptedDescbTv.text = getText(R.string.order_accepted_by_shop)
        mBinding.orderUnacceptedLayoutRl.visibility = View.GONE

        mBinding.deliveryCardView.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE

        mBinding.mapTrack.visibility = View.GONE

        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.GONE

        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.GONE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.GONE

        mBinding.mapTrack.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE

        mBinding.orderOntheWayRl.visibility = View.GONE
        mBinding.unOrderOntheWayRl.visibility = View.GONE

        mBinding.unOrderDeliveryRl.visibility = View.GONE
    }

    private fun stateWhenTakeAwayOrdered() {
        mBinding.orderAcceptedTv.text = getText(R.string.order_approval)
        mBinding.orderAcceptedDescbTv.text = getText(R.string.wait_for_order_approval)
        mBinding.orderAcceptedLayoutRl.visibility = View.VISIBLE
        mBinding.unOrderTakeAwayDeliveryRl.visibility = View.VISIBLE
        mBinding.orderDeliveryTakeAwayHolder.visibility = View.GONE
        mBinding.orderUnacceptedLayoutRl.visibility = View.GONE

        mBinding.deliveryCardView.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE

        mBinding.orderProcessLayoutRl.visibility = View.GONE
        mBinding.orderProcessUnselectLayoutRl.visibility = View.GONE

        mBinding.orderDeliverprsnassignedLayoutRl.visibility = View.GONE
        mBinding.orderDeliverprsnUnassignedLayoutRl.visibility = View.GONE

        mBinding.mapTrack.visibility = View.GONE
        mBinding.deliveryPersonDetailsLabel.visibility = View.GONE

        mBinding.orderOntheWayRl.visibility = View.GONE
        mBinding.unOrderOntheWayRl.visibility = View.GONE

        mBinding.unOrderDeliveryRl.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    private fun stateWhenTakeAway(responseData: ResOrderDetail) {
        val response = responseData.responseData
        mBinding.takeawayCardView.visibility = View.VISIBLE
        mBinding.shopNumber.text = "+91" + response?.pickup?.contact_number.toString() // country code not added in json
        mBinding.shopNameTv.text = response?.store?.store_name.toString()

        mBinding.shopMapTrack.setOnClickListener {
            val orderTrackModel = OrderTrackModel()
            if(orderStatus.equals("STARTED") || orderStatus.equals("REACHED")){
                orderTrackModel.storeLatitude = response?.provider?.latitude
                orderTrackModel.storeLongitude = response?.provider?.longitude
                orderTrackModel.deliveryLatitude = response?.store?.latitude
                orderTrackModel.deliveryLongitude = response?.store?.longitude
            }else{
                orderTrackModel.storeLatitude = response?.provider?.latitude
                orderTrackModel.storeLongitude = response?.provider?.longitude
                orderTrackModel.deliveryLatitude = response?.deliveryaddress?.latitude
                orderTrackModel.deliveryLongitude = response?.deliveryaddress?.longitude
            }

            orderTrackModel.providerId = response?.provider?.id
            orderTrackModel.orderId = response?.id
            // openNewActivity(this, OrderTrackingActivity::class.java, false)
            val intent = Intent(this, OrderTrackingActivity::class.java)
            intent.putExtra("location", orderTrackModel)
            startActivity(intent)
        }
    }


    private fun showHideOTP() {
        if (otpVerifyFlag.equals("1")) {
            mBinding.otpValueTv.visibility = View.VISIBLE
            mBinding.otpLabel.visibility = View.VISIBLE
        } else {
            mBinding.otpValueTv.visibility = View.GONE
            mBinding.otpLabel.visibility = View.GONE
        }

    }

    @SuppressLint("SetTextI18n")
    private fun setOnClickListeners(responseData: ResOrderDetail) {
        val response = responseData.responseData
        mBinding.orderidTv.text = response?.store_order_invoice_id
        mBinding.deliverychargeTv.text = response?.user?.currency_symbol + response?.invoice?.delivery_amount
        mBinding.servicetaxTv.text = response?.user?.currency_symbol + response?.invoice?.tax_amount
        mBinding.addonsTv.text = response?.user?.currency_symbol + response?.invoice?.items!![0]!!.tot_addon_price
        //  mBinding.orderidTv.text = responseData.store_order_invoice_id
        mBinding.otpValueTv.text = response.order_otp
        mBinding.orderDetailListItem = OrderItemListAdapter(this, response.invoice.items
                , response.user?.currency_symbol)
        if (response.provider != null) {
            if (response.provider.picture != null)
                ViewUtils.setImageViewGlide(this, mBinding.deliveryPersonImage, response.provider.picture)
            else
                ViewUtils.setImageViewGlide(this, mBinding.deliveryPersonImage, "")
            mBinding.deliveryPersonNameTv.text = response.provider.first_name + " " + response.provider.last_name
            mBinding.providerNumber.text = response.provider.mobile
        }
        mBinding.totalChargeValueTv.text = response.user?.currency_symbol + " " + response.invoice.total_amount.toString()
        mBinding.packingPriceTv.text = response.user?.currency_symbol + " " + response.invoice.store_package_amount.toString()
        mBinding.discountPriceTv.text = response.user?.currency_symbol + " -" + response.invoice.discount.toString()
        mBinding.tvPromoCode.text = response.user?.currency_symbol + " -" + response.invoice.promocode_amount.toString()
        mBinding.etaTimeTv.text = response.store?.estimated_delivery_time.toString() + " " + getString(R.string.mins)
        mBinding.cancelOrder.setOnClickListener {
            val mReasonFragment = OrderReasonFragment.newInstance(response.id!!)
            mReasonFragment.isCancelable = true
            mReasonFragment.show(supportFragmentManager, mReasonFragment.tag)
        }
        mBinding.chatTv.setOnClickListener {
            mRequestId = response.id
            userId = response.user_id
            providerId = response.provider_id
            userName = response.user?.first_name + " " + response.user?.last_name
            providerName = response.provider?.first_name + " " + response.provider?.last_name

            preference.setValue(Constant.Chat.ADMIN_SERVICE, Constant.ModuleTypes.ORDER)
            preference.setValue(Constant.Chat.USER_ID, userId)
            preference.setValue(Constant.Chat.REQUEST_ID, mRequestId)
            preference.setValue(Constant.Chat.PROVIDER_ID, providerId)
            preference.setValue(Constant.Chat.USER_NAME, userName)
            preference.setValue(Constant.Chat.PROVIDER_NAME, providerName)

            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }
        mBinding.providerNumber.setOnClickListener {
            val phoneNumber = response.provider?.mobile
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        }

        mBinding.mapTrack.setOnClickListener {
            val orderTrackModel = OrderTrackModel()

            if(orderStatus.equals("STARTED") || orderStatus.equals("REACHED")){
                orderTrackModel.storeLatitude = response?.provider?.latitude
                orderTrackModel.storeLongitude = response?.provider?.longitude
                orderTrackModel.deliveryLatitude = response?.store?.latitude
                orderTrackModel.deliveryLongitude = response?.store?.longitude
            }else{
                orderTrackModel.storeLatitude = response?.provider?.latitude
                orderTrackModel.storeLongitude = response?.provider?.longitude
                orderTrackModel.deliveryLatitude = response?.deliveryaddress?.latitude
                orderTrackModel.deliveryLongitude = response?.deliveryaddress?.longitude
            }

            orderTrackModel.providerId = response?.provider?.id
            orderTrackModel.orderId = response?.id
            // openNewActivity(this, OrderTrackingActivity::class.java, false)
            val intent = Intent(this, OrderTrackingActivity::class.java)
            intent.putExtra("location", orderTrackModel)
            startActivity(intent)
        }
    }
}