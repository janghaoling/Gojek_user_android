package com.gox.basemodule.common.payment.managepayment

import android.content.Intent
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.gox.basemodule.R
import com.gox.basemodule.base.BaseActivity
import com.gox.basemodule.databinding.ActivityManagePaymentBinding
import com.gox.basemodule.common.payment.transaction.TransactionFragment
import com.gox.basemodule.common.payment.wallet.WalletFragment
import com.gox.basemodule.common.payment.adapter.PaymentAdapter
import kotlinx.android.synthetic.main.toolbar_base_layout.view.*
import java.util.*

class ManagePaymentActivity : BaseActivity<ActivityManagePaymentBinding>(),
        ManagePaymentNavigator, ViewPager.OnPageChangeListener {

    private lateinit var activityManagePaymentBinding: ActivityManagePaymentBinding
    private lateinit var managePaymentViewModel: ManagePaymentViewModel
    private lateinit var paymentAdapter: PaymentAdapter
    private lateinit var tbManagePayment: TabLayout

    override fun getLayoutId() = R.layout.activity_manage_payment

    override fun initView(mViewDataBinding: ViewDataBinding?) {
        activityManagePaymentBinding = mViewDataBinding as ActivityManagePaymentBinding
        managePaymentViewModel = ViewModelProviders.of(this).get(ManagePaymentViewModel::class.java)
        managePaymentViewModel.navigator = this
        tbManagePayment = findViewById(R.id.tb_payment)
        activityManagePaymentBinding.toolbarLayout.title_toolbar.title = resources.getString(R.string.wallet)
        activityManagePaymentBinding.toolbarLayout.toolbar_back_img.setOnClickListener {
            finish()
        }

        val activityResultflag = intent.getStringExtra("activity_result_flag")
        managePaymentViewModel.setFlag(activityResultflag)
        val paymentFragmentList = Vector<Fragment>()

        val walletFragment = WalletFragment()
        val transactionFragment = TransactionFragment()

        paymentFragmentList.add(walletFragment)
        paymentFragmentList.add(transactionFragment)
        paymentAdapter = PaymentAdapter(supportFragmentManager, this@ManagePaymentActivity
                , paymentFragmentList)
        activityManagePaymentBinding.vbPayment.adapter = paymentAdapter
        tbManagePayment.setupWithViewPager(activityManagePaymentBinding.vbPayment)

    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }

    override fun onPageSelected(position: Int) {
    }

    override fun addCard() {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        loadingObservable.value = false
    }

}