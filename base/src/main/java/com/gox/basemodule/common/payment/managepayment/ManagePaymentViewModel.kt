package com.gox.basemodule.common.payment.managepayment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gox.basemodule.base.BaseViewModel

class ManagePaymentViewModel : BaseViewModel<ManagePaymentNavigator>() {
    val activityFlag = MutableLiveData<String>()
    var strCurrencyType=MutableLiveData<String>()
    fun setFlag(flag: String) {
        activityFlag.value = flag
    }

    fun getFlag(): LiveData<String> {
        return activityFlag
    }

}