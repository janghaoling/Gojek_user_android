package com.gox.foodiemodule.ui.viewCartActivity

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import android.widget.RadioButton
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gox.basemodule.BaseApplication
import com.gox.basemodule.base.BaseActivity
import com.gox.basemodule.common.cardlist.ActivityCardList
import com.gox.basemodule.common.manage_address.ManageAddressActivity
import com.gox.basemodule.common.manage_address.ManageAddressViewModel
import com.gox.basemodule.data.Constant
import com.gox.basemodule.data.PreferenceHelper
import com.gox.basemodule.data.PreferenceKey
import com.gox.basemodule.data.getValue
import com.gox.basemodule.model.AddressModel
import com.gox.basemodule.utils.ViewUtils
import com.gox.foodiemodule.R
import com.gox.foodiemodule.adapter.MenuItemClickListner
import com.gox.foodiemodule.adapter.ViewCartMenuItemListAdapter
import com.gox.foodiemodule.data.model.CartMenuItemModel
import com.gox.foodiemodule.data.model.ReqPlaceOrder
import com.gox.foodiemodule.data.model.ResCheckCartModel
import com.gox.foodiemodule.data.model.ResturantDetailsModel
import com.gox.foodiemodule.databinding.ActivityCartPageBinding
import com.gox.foodiemodule.fragment.coupon.OrderCouponFragment
import com.gox.foodiemodule.fragment.coupon.add_note.AddNoteFragment
import com.gox.foodiemodule.ui.orderdetailactivity.OrderDetailActivity
import kotlinx.android.synthetic.main.foodie_toolbar_layout.view.*
import java.util.*
import kotlin.collections.ArrayList

class ViewCartActivity : BaseActivity<ActivityCartPageBinding>(), ViewCartNavigator {

    private lateinit var mViewDataBinding: ActivityCartPageBinding
    private var products: ArrayList<ResturantDetailsModel.ResponseData.Product> = ArrayList()
    private lateinit var viewCartViewModel: ViewCartViewModel
    private lateinit var mManageAddressViewModel: ManageAddressViewModel
    private var mAddressModel = AddressModel()
    private var addonsId: Int = 0
    private var promoId = 0
    private var paymentMode: String = "CASH"
    private var cardID: String = ""
    private var mUserAddressId: Int? = null
    private var mAddressList: ArrayList<AddressModel>? = null
    private val reqPlaceOrder = ReqPlaceOrder()
    private var menuItemListAdapter: ViewCartMenuItemListAdapter? = null
    override fun getLayoutId(): Int = R.layout.activity_cart_page


    @SuppressLint("SetTextI18n")
    override fun initView(mViewDataBinding: ViewDataBinding?) {
        this.mViewDataBinding = mViewDataBinding as ActivityCartPageBinding
        mViewDataBinding.resturantDetailToolbar.title_toolbar.setTitle(R.string.cart)
        mViewDataBinding.resturantDetailToolbar.toolbar_back_img.setOnClickListener {
            finish()
        }
        mViewDataBinding.resturantDetailToolbar.search_resturant_img.visibility = View.GONE
        mViewDataBinding.paymentTypeCartpageTv.text = getString(R.string.cash)
        viewCartViewModel = ViewModelProviders.of(this).get(ViewCartViewModel::class.java)
        mManageAddressViewModel = ViewModelProviders.of(this).get(ManageAddressViewModel::class.java)
        baseLiveDataLoading = viewCartViewModel.loadingProgress
        mManageAddressViewModel.getAddressList()
        mViewDataBinding.viewCartViewModel = viewCartViewModel
        mViewDataBinding.deliveryRb.isChecked = true
        reqPlaceOrder.orderType = "DELIVERY"
        viewCartViewModel.navigator = this
        mAddressList = ArrayList()
        viewCartViewModel.getCartList()
        viewCartViewModel.getPromocodeDetail()
        mManageAddressViewModel.getAddressListResponse().observe(this, Observer {
            if (it != null) {
                if (it.statusCode == "200") {
                    mAddressList!!.addAll(it.addressList!!)
                    if (mAddressList!!.size > 0) {
                        mUserAddressId = mAddressList!![0].id
                        mViewDataBinding.addressTypeCartpageTv.visibility = View.VISIBLE
                        mViewDataBinding.addressTypeCartpageTv.text = mAddressList!![0].addressType
                        mViewDataBinding.tvAddressLine.text = mAddressList!![0].flatNumber.toString() + "," +
                                mAddressList!![0].street.toString() + "," + mAddressList!![0].landmark.toString()
                    } else {
                        mViewDataBinding.addressTypeCartpageTv.visibility = View.GONE
                        mViewDataBinding.tvAddressLine.text = getString(R.string.noaddressfoundpleaseaddaddress)
                    }
                }
            }
        })
        mViewDataBinding.deliveryTypeRg.setOnCheckedChangeListener { _, checkedId ->
            val radio: RadioButton = this.findViewById(checkedId)
            if (radio.text == "Delivery") {
                reqPlaceOrder.orderType = "DELIVERY"
            } else {
                reqPlaceOrder.orderType = "TAKEAWAY"
            }

        }
        mViewDataBinding.cbUseWalletAmount.isChecked = false

        if (mViewDataBinding.cbUseWalletAmount.isChecked) {
            reqPlaceOrder.wallet = "1"
        } else {
            reqPlaceOrder.wallet = "0"
        }

        mViewDataBinding.cbUseWalletAmount.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                reqPlaceOrder.wallet = "1"
            } else {
                reqPlaceOrder.wallet = "0"
            }
        }

        viewCartViewModel.checkoutResponse.observe(this, Observer {
                goToOrderDetailPage(it)
        })
        viewCartViewModel.errorResponse.observe(this, Observer {
            if (it != null) {
                ViewUtils.showToast(this, "" + it, false)
            }
        })
        viewCartViewModel.cartListResponse.observe(this, Observer<CartMenuItemModel> {
            products.clear()
            if (it.responseData!!.carts!!.isNotEmpty()) {
                mViewDataBinding.cartEmptyView.visibility = View.GONE
                mViewDataBinding.cartView.visibility = View.VISIBLE
                mViewDataBinding.bottomLayout.visibility = View.VISIBLE
                updateWalletBalance(it.responseData.wallet_balance)
                for (i in it.responseData.carts!!.indices) {
                    products.add(it.responseData.carts[i]!!.product!!)
                    products[i].tot_quantity = it.responseData.carts[i]!!.quantity
                    products[i].cartId = it.responseData.carts[i]!!.id
                    products[i].total_item_price = it.responseData.carts[i]!!.total_item_price
                }
//                menuItemListAdapter = MenuItemListAdapter(this, products, "viewcart")
                menuItemListAdapter = ViewCartMenuItemListAdapter(this, products, "viewcart")
                mViewDataBinding.viewCartmenuItemListAdapter = menuItemListAdapter
                menuItemListAdapter!!.notifyDataSetChanged()
                menuItemListAdapter!!.setOnClickListener(mOnMenuItemClickListner)
                mViewDataBinding.totalChargeValueTv.text = it.responseData.user_currency +" "+it.responseData.payable.toString()
                mViewDataBinding.discountPrice.text = it.responseData.user_currency + " - " + it.responseData.shop_discount.toString()
                mViewDataBinding.deliveryChargePrice.text = it.responseData.user_currency + " " + it.responseData.delivery_charges.toString()
                mViewDataBinding.shopPackageCharge.text = it.responseData.user_currency + " " + it.responseData.shop_package_charge.toString()
                mViewDataBinding.taxCharge.text = it.responseData.user_currency + " " + it.responseData.shop_gst_amount.toString()
                mViewDataBinding.restaturantName.text = it.responseData.carts[0]!!.store!!.store_name
                mViewDataBinding.hotelRatingTv.text = it.responseData.rating
                if(it.responseData.promocode_amount.toString()!="0.0"){
                    mViewDataBinding.applyCouponTv.text = it.responseData.promocode_amount.toString() +" (PromoCode Applied)"
                }else{
                    mViewDataBinding.applyCouponTv.text = "Apply Coupon"
                }

                ViewUtils.setImageViewGlide(this, mViewDataBinding.resturantImage,
                        it.responseData.carts[0]!!.store!!.picture!!)
            } else {
                mViewDataBinding.cartEmptyView.visibility = View.VISIBLE
                mViewDataBinding.cartView.visibility = View.GONE
                mViewDataBinding.bottomLayout.visibility = View.GONE
                finish()
            }


        })
        mViewDataBinding.changeAddressCartTv.setOnClickListener {

            val intent = Intent(this, ManageAddressActivity::class.java)
            intent.putExtra("changeAddressFlag", "1")
            startActivityForResult(intent, Constant.CHANGE_ADDRESS_TYPE_REQUEST_CODE)
        }
        mViewDataBinding.applyCouponTv.setOnClickListener {
            if(mViewDataBinding.applyCouponTv.text.equals("Apply Coupon")){
                val mTaxiCouponFragment = OrderCouponFragment.newInstance()
                mTaxiCouponFragment.show(supportFragmentManager, mTaxiCouponFragment.tag)
            }
            else{
            viewCartViewModel.getCartList()
        }
        }
        mViewDataBinding.changePaymentCartTv.setOnClickListener {
            val intent = Intent(this@ViewCartActivity, ActivityCardList::class.java)
            intent.putExtra("activity_result_flag", "1")
            startActivityForResult(intent, Constant.PAYMENT_TYPE_REQUEST_CODE)
        }

        viewCartViewModel.getSelectedPromo().observe(this, Observer {
            if (it != null) {
                promoId = it.id!!
                viewCartViewModel.applyPromoCode(promoId)
            }
        })
        mViewDataBinding.tvAddNote.setOnClickListener {
            val mAddNoteFragment = AddNoteFragment.newInstance()
            mAddNoteFragment.isCancelable = true
            mAddNoteFragment.show(supportFragmentManager, mAddNoteFragment.tag)
        }


    }

    private fun goToOrderDetailPage(it: ResCheckCartModel) {
        val intent = Intent(this@ViewCartActivity, OrderDetailActivity::class.java)
        intent.putExtra("orderId", it.responseData?.id)
        startActivity(intent)
        finish()
    }

    private fun updateWalletBalance(amount: Double) {
        if (amount > 0) {
            mViewDataBinding.walletLayout.visibility = View.VISIBLE
            mViewDataBinding.tvWalletAmount.visibility = View.VISIBLE
            mViewDataBinding.tvWalletAmount.text = amount.toString()
        } else {
            mViewDataBinding.walletLayout.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_CANCELED) {
            when (requestCode) {
                Constant.PAYMENT_TYPE_REQUEST_CODE -> {
                    val paymentType = data?.extras?.get("payment_type").toString()
                    cardID = data?.extras?.get("card_id").toString()
                    if (paymentType.toUpperCase(Locale.getDefault()) == (Constant.PaymentMode.CASH)) {
                        mViewDataBinding.paymentTypeCartpageTv.text = getString(R.string.cash)
                        mViewDataBinding.tvCardNumber.visibility = View.GONE
                        paymentMode = "CASH"
                    } else {
                        mViewDataBinding.paymentTypeCartpageTv.text = getString(R.string.card)
                      //  mViewDataBinding.tvCardNumber.visibility = View.VISIBLE
                       // mViewDataBinding.tvCardNumber.text = "CARD"
                        paymentMode = "CARD"
                    }
                }
                Constant.CHANGE_ADDRESS_TYPE_REQUEST_CODE -> {
                    mAddressModel = data?.extras!!.get("address") as AddressModel
                    mUserAddressId = mAddressModel.id
                    mViewDataBinding.addressTypeCartpageTv.visibility = View.VISIBLE
                    mViewDataBinding.addressTypeCartpageTv.text = mAddressModel.addressType
                    mViewDataBinding.tvAddressLine.text = mAddressModel.flatNumber.toString() + "," +
                            mAddressModel.street.toString() +
                            "," + mAddressModel.landmark.toString()
                    //ViewUtils.showToast(this, "Address Added" + mAddressModel.landmark, false)

                }
            }
        }
    }

    override fun goToOrderPage() {

        reqPlaceOrder.paymentMode = paymentMode
        reqPlaceOrder.card_id = cardID
        reqPlaceOrder.promocodeId = ""
//        reqPlaceOrder.wallet = ""

        if (mUserAddressId != null) {
            reqPlaceOrder.userAddressId = mUserAddressId
            viewCartViewModel.checkOut(reqPlaceOrder)
        } else {
            ViewUtils.showNormalToast(this, "Select the delivery address")
        }


    }


    private val mOnMenuItemClickListner = object : MenuItemClickListner {
        override fun addFilterType(filterType: String) {

        }

        override fun showAddonLayout(position: Int?, itemCount: Int
                                     , itemsaddon: ResturantDetailsModel.ResponseData.Product?, b: Boolean) {

        }

        override fun addedAddons(position: Int) {

        }

        override fun removedAddons(position: Int) {
        }

        override fun addToCart(id: Int?, itemCount: Int, cartId: Int?, repeat: Int, customize: Int) {
            viewCartViewModel.addItemToCart(id!!, cartId!!, itemCount, addonsId,repeat,customize)
        }


        override fun removeCart(position: Int) {
            viewCartViewModel.removeCart(products[position].cartId!!)
            menuItemListAdapter!!.notifyDataSetChanged()
        }


    }

}