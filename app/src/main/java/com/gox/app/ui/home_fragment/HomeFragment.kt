package com.gox.app.ui.home_fragment

import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.gox.basemodule.BaseApplication
import com.gox.basemodule.base.BaseFragment
import com.gox.basemodule.data.PreferenceKey
import com.gox.basemodule.data.PreferenceHelper
import com.gox.basemodule.data.Constant
import com.gox.basemodule.data.setValue
import com.gox.basemodule.utils.ViewUtils
import com.gox.foodiemodule.ui.restaurantlist_activity.RestaurantListActivity
import com.gox.taximodule.ui.activity.main.TaxiMainActivity
import com.gox.app.R
import com.gox.app.adapter.RecommendedServiceAdapter
import com.gox.app.adapter.ServiceListAdapter
import com.gox.app.callbacks.OnClickListener
import com.gox.app.data.repositary.remote.model.City
import com.gox.app.data.repositary.remote.model.HomeMenuResponse
import com.gox.app.databinding.FragmentHomeBinding
import com.gox.app.ui.cityListActivity.CityListActivity
import com.gox.app.ui.viewCouponActivity.ViewCouponActivity
import com.gox.basemodule.extensions.observeLiveData
import com.gox.xubermodule.data.model.XuberServiceClass
import com.gox.xubermodule.ui.activity.mainactivity.XuberMainActivity
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.Serializable

class HomeFragment : BaseFragment<FragmentHomeBinding>(), HomeFragmentNavigator {

    lateinit var mViewDataBinding: FragmentHomeBinding
    private val mMenuList: MutableList<HomeMenuResponse.ResponseData.Service> = ArrayList()
    private val mFeaturedServiceList: MutableList<HomeMenuResponse.ResponseData.Service> = ArrayList()
    private val mCoupons: ArrayList<HomeMenuResponse.ResponseData.Promocode> = ArrayList()
    private var cityList: ArrayList<City> = ArrayList()
    private var mServiceAdapter: ServiceListAdapter? = null
    private var mCountryName: String? = null
    private var mCountryId: String? = null
    private val preference = PreferenceHelper(BaseApplication.baseApplication)
    var homeFragmentViewModel = HomeFragmentViewModel()
    //   private var offersCouponsAdapter: OffersCouponsAdapter? = null
    private var mhomeOfferAdapter: HomeCouponAdapter? = null

    private object TAG {
        val SHOW_LESS = "showLess"
        val SHOW_MORE = "showMore"
    }

    override fun getLayoutId(): Int = R.layout.fragment_home

    @SuppressLint("SetTextI18n")
    override fun initView(mRootView: View?, mViewDataBinding: ViewDataBinding?) {
        this.mViewDataBinding = mViewDataBinding as FragmentHomeBinding
        homeFragmentViewModel.navigator = this
        mViewDataBinding.homefragmentmodel = homeFragmentViewModel
        mhomeOfferAdapter = HomeCouponAdapter(activity!!, mCoupons)
        homeFragmentViewModel = ViewModelProviders.of(this).get(HomeFragmentViewModel::class.java)
        homeFragmentViewModel.getMenu()
        homeFragmentViewModel.getProfile()
        homeFragmentViewModel.mSuccessResponse.observe(this, Observer {
            if (it != null) {
                if (it.statusCode.equals("200")) {
                    ViewUtils.showToast(activity!!, it.message!!, true)
                    homeFragmentViewModel.getMenu()

                }
            }
        })

        homeFragmentViewModel.getMenuResponse().observe(this, Observer {
            if (it != null) {
                if (it.statusCode == "200") {
                    mMenuList.clear()
                    mCoupons.clear()
                    mMenuList.addAll(it.responseData.services)
                    mCoupons.addAll(it.responseData.promocodes)

                    if (mMenuList.isEmpty()) {
                        mViewDataBinding.servicelistFrghomeRv.visibility = View.GONE
                        mViewDataBinding.showmoreFrghomeTv.visibility = View.GONE
                        mViewDataBinding.tvNoServiceFound.visibility = View.VISIBLE
                    } else {
                        mViewDataBinding.servicelistFrghomeRv.visibility = View.VISIBLE
                        mViewDataBinding.showmoreFrghomeTv.visibility = View.VISIBLE
                        mViewDataBinding.tvNoServiceFound.visibility = View.GONE
                    }

                    mServiceAdapter = ServiceListAdapter(activity, mMenuList)
                    mViewDataBinding.serviceListAdapter = mServiceAdapter
                    //   offersCouponsAdapter = OffersCouponsAdapter(activity!!)
                    val featuredList = mMenuList.filter { it.is_featured == 1 }
                    mFeaturedServiceList.addAll(featuredList)
                    val recommendedServiceAdapter = RecommendedServiceAdapter(activity!!,mFeaturedServiceList)
                    recommendedServiceAdapter.setOnClickListener(mOnFeatureAdapterClickListener)
                    mViewDataBinding.recommendedSerVicesAdapter = recommendedServiceAdapter
                    if(mFeaturedServiceList.isEmpty()){
                        emptyRecommendedService.visibility = View.VISIBLE
                        recommended_servicelist_frghome_rv.visibility = View.GONE
                    }else{
                        emptyRecommendedService.visibility = View.GONE
                        recommended_servicelist_frghome_rv.visibility = View.VISIBLE
                    }

                    mhomeOfferAdapter = HomeCouponAdapter(activity!!, mCoupons)
                    mViewDataBinding.homeCouponAdapter = mhomeOfferAdapter
                    if(mViewDataBinding.serviceListAdapter!!.itemCount>0){
                        mViewDataBinding.serviceListAdapter!!.notifyItemRangeRemoved(0,mMenuList.size)
                        mViewDataBinding.serviceListAdapter!!.notifyItemRangeInserted(0,mMenuList.size)
                    }

                    mhomeOfferAdapter!!.notifyDataSetChanged()
                    mServiceAdapter!!.setOnClickListener(mOnAdapterClickListener)
                    mhomeOfferAdapter!!.setOnClickListener(mOnImageAdapterClickListener)
                }

            }
        })

        homeFragmentViewModel.countryListResponse.observe(this, Observer {
            if (it != null) {
                if (it.statusCode == "200") {
                    mViewDataBinding.locationHomefragmentTv.text = homeFragmentViewModel.mProfileResponse.value!!.profileData!!.cityName?.cityName + "," + mCountryName
                    for (i in 0 until it.responseData.size) {
                        if (it.responseData[i].id.toString() == mCountryId) {
                            cityList = it.responseData[i].city as ArrayList<City>
                        }
                    }

                }
            }
        })

        mViewDataBinding.viewPagerCoupons.clipToPadding = false;
        // set padding manually, the more you set the padding the more you see of prev & next page
        mViewDataBinding.viewPagerCoupons.setPadding(40, 0, 40, 0)
        // sets a margin b/w individual pages to ensure that there is a gap b/w them
        mViewDataBinding.viewPagerCoupons.pageMargin = 20;
        // Set margin for pages as a negative number, so a part of next and
        // previous pages will be showed
        mViewDataBinding.rlServiceRoot.tag = TAG.SHOW_MORE
        homeFragmentViewModel.mProfileResponse.observe(this, Observer {
            if (it.statusCode == "200") {
                mCountryName = it.profileData?.countryName?.CountryName
                mCountryId = it.profileData?.countryName?.id
                Constant.currency = it.profileData!!.currencySymbol.toString()
                preference.setValue(PreferenceKey.CURRENCY, it.profileData!!.currencySymbol)
                preference.setValue(PreferenceKey.WALLET_BALANCE, it.profileData!!.walletBalance.toInt())
                preference.setValue(PreferenceKey.CITY_ID, it.profileData!!.cityName?.id)
                homeFragmentViewModel.getProfileCountryList()
            }
        })



        mViewDataBinding.locationHomefragmentTv.setOnClickListener {
            val intent = Intent(activity!!, CityListActivity::class.java)
            intent.putExtra("selectedfrom", "city")
            intent.putExtra("citylistresponse", cityList as Serializable)
            startActivityForResult(intent, Constant.CITYLIST_REQUEST_CODE)
        }


        /* this.mViewDataBinding.materialLeanBack.setOnItemClickListener(object : MaterialLeanBack.OnItemClickListener {
             override fun onItemClicked(row: Int, column: Int) {

                 openNewActivity(activity, ViewCouponActivity::class.java, false)
             }

             override fun onTitleClicked(row: Int, text: String?) {

             }

         })*/

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_CANCELED) {
            when (requestCode) {
                Constant.CITYLIST_REQUEST_CODE -> {
                    setCity(data)
                }
            }
        }
    }

    private fun setCity(data: Intent?) {
        val selectedCity = data?.extras?.get("selected_list") as? City
        val cityId = selectedCity!!.id
        mViewDataBinding.locationHomefragmentTv.text = selectedCity?.city_name + ", " + mCountryName
        homeFragmentViewModel.updateCity(cityId)
    }

    private val mOnAdapterClickListener = object : OnClickListener {
        override fun onClick(position: Int) {
            val mMenuModel = mMenuList[position]
            if (mMenuModel.service.admin_service == "TRANSPORT") {
                val intent = Intent(activity, TaxiMainActivity::class.java)
                intent.putExtra("serviceId", mMenuModel.menu_type_id)
                startActivity(intent)
            } else if (mMenuModel.service.admin_service.equals("Service", true)) {
                val intent = Intent(activity, XuberMainActivity::class.java)
                XuberServiceClass.serviceID = mMenuModel.menu_type_id
                XuberServiceClass.serviceName = mMenuModel.title
                startActivity(intent)
            } else if (mMenuModel.service.admin_service.equals("ORDER", true)) {
                val intent = Intent(activity, RestaurantListActivity::class.java)
                intent.putExtra("serviceId", mMenuModel.menu_type_id)
                startActivity(intent)

            }
        }
    }


    private val mOnFeatureAdapterClickListener = object : OnClickListener {
        override fun onClick(position: Int) {
            val mMenuModel = mMenuList[position]
            if (mMenuModel.service.admin_service == "TRANSPORT") {
                val intent = Intent(activity, TaxiMainActivity::class.java)
                intent.putExtra("serviceId", mMenuModel.menu_type_id)
                startActivity(intent)
            } else if (mMenuModel.service.admin_service.equals("Service", true)) {
                val intent = Intent(activity, XuberMainActivity::class.java)
                XuberServiceClass.serviceID = mMenuModel.menu_type_id
                XuberServiceClass.serviceName = mMenuModel.title
                startActivity(intent)
            } else if (mMenuModel.service.admin_service.equals("ORDER", true)) {
                val intent = Intent(activity, RestaurantListActivity::class.java)
                intent.putExtra("serviceId", mMenuModel.menu_type_id)
                startActivity(intent)

            }
        }
    }

    private val mOnImageAdapterClickListener = object : OnClickListener {
        override fun onClick(position: Int) {
            val promocodeModel = mCoupons[position]
            val intent = Intent(activity, ViewCouponActivity::class.java)
            intent.putExtra("promocode", promocodeModel as Serializable)
            startActivity(intent)

        }
    }

    override fun showMoreLess() {
        val params: ViewGroup.LayoutParams = mViewDataBinding.rlServiceRoot.layoutParams
        if (mViewDataBinding.rlServiceRoot.tag == TAG.SHOW_MORE) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = resources.getDimensionPixelSize(R.dimen._280sdp)
            mViewDataBinding.rlServiceRoot.layoutParams = params
            mViewDataBinding.rlServiceRoot.tag = TAG.SHOW_LESS
            mViewDataBinding.showmoreFrghomeTv.text = getString(R.string.user_show_less)
        } else {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = resources.getDimensionPixelSize(R.dimen._150sdp)
            mViewDataBinding.rlServiceRoot.layoutParams = params
            mViewDataBinding.rlServiceRoot.tag = TAG.SHOW_MORE
            mViewDataBinding.showmoreFrghomeTv.text = getString(R.string.user_show_more)
        }
    }
}
