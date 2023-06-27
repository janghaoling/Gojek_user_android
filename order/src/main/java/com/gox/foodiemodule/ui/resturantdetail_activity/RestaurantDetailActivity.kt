package com.gox.foodiemodule.ui.resturantdetail_activity

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.gox.basemodule.BaseApplication
import com.gox.basemodule.base.BaseActivity
import com.gox.basemodule.data.Constant
import com.gox.basemodule.data.PreferenceHelper
import com.gox.basemodule.data.getValue
import com.gox.basemodule.data.setValue
import com.gox.basemodule.utils.ViewUtils
import com.gox.foodiemodule.R
import com.gox.foodiemodule.adapter.AddonsListAdapter
import com.gox.foodiemodule.adapter.MenuItemClickListner
import com.gox.foodiemodule.adapter.MenuItemListAdapter
import com.gox.foodiemodule.data.model.CartMenuItemModel
import com.gox.foodiemodule.data.model.ResturantDetailsModel
import com.gox.foodiemodule.databinding.ActivityRestaurantdetailLayoutBinding
import com.gox.foodiemodule.ui.viewCartActivity.ViewCartActivity
import kotlinx.android.synthetic.main.foodie_toolbar_layout.view.*


class RestaurantDetailActivity : BaseActivity<ActivityRestaurantdetailLayoutBinding>(), ResturantDetailNavigator {

    var showViewCartAlert: Boolean = false
    var cartId: Int = 0
    var addonsId: String = "0"
    lateinit var mViewDataBinding: ActivityRestaurantdetailLayoutBinding
    lateinit var restaurantDetailViewModel: RestaturantDetailViewModel
    private var restaurantsId: String? = null
    var addonsList: ArrayList<Int> = ArrayList<Int>()
    val preference = PreferenceHelper(BaseApplication.baseApplication)

    var products: List<ResturantDetailsModel.ResponseData.Product?>? = listOf()


    override fun getLayoutId(): Int = R.layout.activity_restaurantdetail_layout
    override fun initView(mViewDataBinding: ViewDataBinding?) {

        this.mViewDataBinding = mViewDataBinding as ActivityRestaurantdetailLayoutBinding
        mViewDataBinding.lifecycleOwner = this

        restaurantsId = intent.getStringExtra("restaurantsId")
        preference.setValue("restaurantsId", restaurantsId.toString())
        mViewDataBinding.resturantDetailToolbar.search_resturant_img.visibility = View.GONE;
        mViewDataBinding.resturantDetailToolbar.toolbar_back_img.setOnClickListener { finish() }
        restaurantDetailViewModel = ViewModelProviders
                .of(this).get(RestaturantDetailViewModel::class.java)
        restaurantDetailViewModel.navigator = this
        mViewDataBinding.resturantDetailViewModel = restaurantDetailViewModel

        restaurantDetailViewModel.loadingProgress.observe(this@RestaurantDetailActivity, Observer {
            baseLiveDataLoading.value = it
        })

        restaurantDetailViewModel.resturantDetailResponse.observe(this@RestaurantDetailActivity,
                Observer<ResturantDetailsModel> {
                    restaurantDetailViewModel.loadingProgress.value = false
                    hideLoading()
                    if (it.responseData != null) {
                        setResturantDetails(it.responseData)
                    }

                })

        restaurantDetailViewModel.cartMenuItemResponse.observe(this@RestaurantDetailActivity,
                Observer<CartMenuItemModel> {
                    Log.d("updatecart", it.responseData!!.store_type)

                    restaurantDetailViewModel.getResturantDetails(restaurantsId!!, "", "all")

/*                    if (it.statusCode == "200") {
                        updateViewCart(it.responseData!!)
                    } else {
                        ViewUtils.showToast(this, "Cart not updated", false)
                    }*/
                })

        restaurantDetailViewModel.cartRemoveResponse.observe(this@RestaurantDetailActivity, Observer {
            if (it.statusCode == "200") {

                restaurantDetailViewModel.getResturantDetails(restaurantsId!!, "", "all")

            } else {
                ViewUtils.showToast(this, "Cart not updated", false)
            }
        })

        mViewDataBinding.restaturantTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(position){
                    0 ->{
                        restaurantDetailViewModel.getResturantDetails(restaurantsId!!, "", "pure-veg")
                    }

                    1 ->{
                        restaurantDetailViewModel.getResturantDetails(restaurantsId!!, "", "non-veg")
                    }

                    2 ->{
                        restaurantDetailViewModel.getResturantDetails(restaurantsId!!, "", "all")
                    }

                }
            }

        }

    }

    override fun onResume() {
        super.onResume()
        restaurantsId = preference.getValue("restaurantsId", restaurantsId!!.toString()) as String
        restaurantDetailViewModel.getResturantDetails(restaurantsId!!, "", "all")
    }

    @SuppressLint("SetTextI18n")
    private fun updateViewCart(cartData: CartMenuItemModel.ResponseData) {

        if (cartData.total_cart!! > 0) {
            mViewDataBinding.viewCartLayout.visibility = View.VISIBLE
            mViewDataBinding.totalItemsCountPriceTv
            mViewDataBinding.totalItemsCountPriceTv.text = " " + cartData.total_cart + getString(R.string.items) +
                    Constant.currency + cartData.total_price

        } else {
            mViewDataBinding.viewCartLayout.visibility = View.GONE
        }

        cartId = cartData.carts!![0]!!.id!!

    }

    @SuppressLint("SetTextI18n")
    private fun setResturantDetails(resturantDetail: ResturantDetailsModel.ResponseData) {

        if (resturantDetail.storetype!!.name!!.contains("Foo", true)) {
            mViewDataBinding.restaturantTypeSpinner.visibility = View.VISIBLE
        } else {
            mViewDataBinding.restaturantTypeSpinner.visibility = View.GONE

        }


        if (resturantDetail.usercart!! > 0) {
            mViewDataBinding.viewCartLayout.visibility = View.VISIBLE

            val itemsFound = resources.getQuantityString(R.plurals.numberOfItems, resturantDetail.usercart, resturantDetail.usercart)
            mViewDataBinding.totalItemsCountPriceTv.text = "" + itemsFound + " " + Constant.currency + resturantDetail.totalcartprice


        } else {
            mViewDataBinding.viewCartLayout.visibility = View.GONE
        }

        if (resturantDetail.totalstorecart == 0 && resturantDetail.usercart > 0) {
            showViewCartAlert = true
        }



        mViewDataBinding.restaturantName.text = resturantDetail.store_name
        mViewDataBinding.resturantDetailToolbar.title_toolbar.title = resturantDetail.store_name

        var categoryName = ""
        for (i in resturantDetail.categories!!.indices) {
            if (i == 0)
                categoryName = resturantDetail.categories[i]!!.store_category_name!!
            else
                categoryName = categoryName + ", " + resturantDetail.categories[i]!!.store_category_name
        }
        mViewDataBinding.restaturantCusinetypeTv.text = categoryName

        /*if (resturantDetail.categories?.size!! > 0)
            mViewDataBinding.restaturantCusinetypeTv.text = resturantDetail.categories.get(0)?.store_category_description*/
        mViewDataBinding.etaTimeTv.text = resturantDetail.estimated_delivery_time + " " + getString(R.string.mins)
        mViewDataBinding.ratingTv.text = resturantDetail.rating.toString() + ""
        mViewDataBinding.minamountTv.text = Constant.currency + "" + resturantDetail.offer_min_amount.toString()

        /*  products.clear()
          for (i in 0 until resturantDetail!!.carts!!.size) {


              products.add(resturantDetail.carts!![i]!!.product!!)
              products[i].tot_quantity = resturantDetail.carts[i]!!.quantity
              products[i].cartId = resturantDetail.responseData.carts[i]!!.id

          }*/

        products = resturantDetail.products

        val menuItemListAdapter = MenuItemListAdapter(this, resturantDetail, "")
        mViewDataBinding.menuItemListAdapter = menuItemListAdapter
        mViewDataBinding.menuItemListAdapter!!.notifyDataSetChanged()
        menuItemListAdapter.setOnClickListener(mOnMenuItemClickListner)

    }

    override fun goToCartPage() {

        openNewActivity(this, ViewCartActivity::class.java, false)

    }

    private val mOnMenuItemClickListner = object : MenuItemClickListner {
        override fun addFilterType(filterType: String) {

        }


        override fun showAddonLayout(id: Int?, itemCount: Int, itemsaddon: ResturantDetailsModel.ResponseData.Product?
                                     , isContainAddon: Boolean) {
            if (showViewCartAlert) {
                mshowViewCartAlert(id!!, cartId, itemCount, addonsId, itemsaddon)
            } else {
                if (isContainAddon)
                    showAddonBottomSheet(id, itemCount, itemsaddon!!)
                else
                    restaurantDetailViewModel.addItemToCart(id!!, 0, itemCount, addonsId, 0, 0)
            }

        }

        override fun addToCart(id: Int?, itemCount: Int, cartId: Int?, repeat: Int, customize: Int) {
            if (showViewCartAlert) {
                mshowViewCartAlert(id!!, cartId!!, itemCount, addonsId, null)
            } else {
                restaurantDetailViewModel.addItemToCart(id!!, cartId!!, itemCount, addonsId, repeat,customize)            }
        }



        override fun removeCart(position: Int) {
            val itemCart = products?.get(position)?.itemcart

            if (itemCart?.size == 1)
                restaurantDetailViewModel.removeCart(products?.get(position)?.itemcart?.get(0)?.id!!)
            else {
                goToCartPage()
            }

        }

        override fun addedAddons(position: Int) {
            Log.d("_D", "itemaddonId" + position)
            addonsList.add(position)
        }

        override fun removedAddons(position: Int) {
            addonsList.remove(position)

        }


    }

    private fun mshowViewCartAlert(id: Int, cartId: Int, itemCount: Int, addonsId: String, itemsaddon: ResturantDetailsModel.ResponseData.Product?) {
        val builder = AlertDialog.Builder(this@RestaurantDetailActivity)

        // Set the alert dialog title
        builder.setTitle("Replace cart item?")

        // Display a message on alert dialog
        builder.setMessage("Your cart contains items from another ${Constant.storetype}.Do you want discard the selection?")

        // Set a positive button and its click listener on alert dialog
        builder.setPositiveButton("YES") { dialog, which ->

            showViewCartAlert = false
            if (itemsaddon != null) {
                showAddonBottomSheet(id, itemCount, itemsaddon)
            }
            restaurantDetailViewModel.addItemToCart(id, cartId, itemCount, addonsId, 0,0)
        }

        builder.setNegativeButton("NO") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()


        }

        val dialog: AlertDialog = builder.create()
        dialog.show()


    }

    @SuppressLint("SetTextI18n")
    private fun showAddonBottomSheet(id: Int?, itemCount: Int, product: ResturantDetailsModel.ResponseData.Product) {
        val inflate = DataBindingUtil.inflate<com.gox.foodiemodule.databinding
        .FoodieAddonsDialogBinding>(LayoutInflater.from(baseContext)
                , com.gox.foodiemodule.R.layout.foodie_addons_dialog, null, false)

        val addonsAdapter = AddonsListAdapter(product.itemsaddon)
        ViewUtils.setImageViewGlide(this@RestaurantDetailActivity, inflate.itemImg
                , product.picture.toString())
        inflate.itemPriceTv.text = Constant.currency + product.item_price
        inflate.itemNameTv.text = product.item_name
        inflate.addonsAdapter = addonsAdapter
        addonsAdapter.setOnClickListener(mOnMenuItemClickListner)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(inflate.root)
        dialog.setOnCancelListener(
                DialogInterface.OnCancelListener {
                    //When you touch outside of dialog bounds,
                    //the dialog gets canceled and this method executes.
                    restaurantDetailViewModel.addItemToCart(id!!, 0, itemCount, addonsId, 0,0)
                    dialog.dismiss()
                }
        )
        inflate.applyFilter.setOnClickListener {
            addonsId = ""
            if (addonsList.size > 0) {
                for (i in 0 until addonsList.size) {
                    if (i == 0)
                        addonsId = ""+ addonsList[i]
                    else
                        addonsId += "," + addonsList[i]
                }
                addonsList.clear()
            }
            restaurantDetailViewModel.addItemToCart(id!!, 0, itemCount, addonsId, 0,0)
            dialog.dismiss()
        }


        inflate.closeDialogImg.setOnClickListener {
            restaurantDetailViewModel.addItemToCart(id!!, 0, itemCount, addonsId, 0,0)
            dialog.dismiss()

        }

        dialog.show()
    }


}
