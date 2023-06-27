package com.gox.app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.gox.app.R
import com.gox.app.data.repositary.remote.model.DisputeListData
import com.gox.app.databinding.DisputeRowItemBinding
import com.gox.app.ui.historydetailactivity.HistoryDetailViewModel
import java.util.*


class DisputeReasonListAdapter(val historyDetailViewModel: HistoryDetailViewModel, val disputereasonList: List<DisputeListData>)
    : RecyclerView.Adapter<DisputeReasonListAdapter.MyViewHolder>() {

    private var mOnAdapterClickListener: ReasonListClicklistner? = null
    private var selectedPosition = -1

    fun setOnClickListener(onClickListener: ReasonListClicklistner) {
        this.mOnAdapterClickListener = onClickListener
    }

    lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        this.context = parent.context
        val inflate = DataBindingUtil
                .inflate<DisputeRowItemBinding>(LayoutInflater.from(parent.context),
                        R.layout.dispute_row_item, parent, false)
        return MyViewHolder(inflate)
    }

    override fun getItemCount(): Int = disputereasonList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.disputeRowItemBinding.rbDisbute.isChecked = selectedPosition == position
        holder.disputeRowItemBinding.tvDisbuteReason.text = (disputereasonList[position]
                .dispute_name).toUpperCase(Locale.getDefault())

        holder.disputeRowItemBinding.rbDisbute.setOnCheckedChangeListener { _, _ ->
            onItemChecked(position)
        }
        holder.disputeRowItemBinding.llDisputeReaons.setOnClickListener {
            onItemChecked(position)
        }

    }

    private fun onItemChecked(position: Int) {
        val oldIndex = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldIndex)
        notifyItemChanged(selectedPosition)
        if (mOnAdapterClickListener != null) {
            mOnAdapterClickListener!!.reasonOnItemClick((disputereasonList.get(position)
                    .dispute_name).toUpperCase(Locale.getDefault()))
        }
    }


    inner class MyViewHolder(itemView: DisputeRowItemBinding) : RecyclerView.ViewHolder(itemView.root) {

        val disputeRowItemBinding = itemView

        fun bind() {

        }


    }


}


