package com.heng.chatinput

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import com.heng.chatinput.ChatInputMoreFragment.MoreListener

/**
 * 更多模块的Fragment工厂类
 */
interface MoreFragmentFactory {
  fun create(
    moreListener: MoreListener,
    moreModuleList: List<MoreModuleBean>
  ): Fragment
}

internal class DefaultMoreFragmentFactory : MoreFragmentFactory {
  override fun create(
    moreListener: MoreListener,
    moreModuleList: List<MoreModuleBean>
  ): Fragment {
    return ChatInputMoreFragment().apply {
      this.moreListener = moreListener
      this.dataList = moreModuleList
    }
  }

}

/**
 * 聊天室 输入框 的更多面板
 *
 */
class ChatInputMoreFragment : Fragment() {
  interface MoreListener {
    fun onMoreModule(moreModule: MoreModuleBean)
  }

  lateinit var moreListener: MoreListener
  private var _dataList = mutableListOf<MoreModuleBean>()
  var dataList: List<MoreModuleBean>? = null
    set(value) {
      if (value != null && value.isNotEmpty()) {
        _dataList.clear()
        _dataList.addAll(value)
        adapter?.notifyDataSetChanged()
      }
    }
  private var adapter: BaseAdapter? = null
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_chat_input_more, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    val gridView = view.findViewById<GridView>(R.id.gridview)
    adapter = object : BaseAdapter() {
      override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup?
      ): View? {
        val itView =
          convertView ?: LayoutInflater.from(context).inflate(
              R.layout.item_chat_input_more, null
          ).apply {
            this.layoutParams =
              AbsListView.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT
                  , EmotionKeyboard.getKeyBoardHeight() / 2
              )
          }
        itView?.findViewById<ImageView>(R.id.img)
            ?.setImageResource(getItem(position).resId)
        itView?.findViewById<TextView>(R.id.txt)
            ?.text = getItem(position).name
        return itView
      }

      override fun getItem(position: Int) = _dataList[position]

      override fun getItemId(position: Int) = position.toLong()

      override fun getCount() = _dataList.size
    }
    gridView.adapter = adapter
    gridView.setOnItemClickListener { parent, view, position, id ->
      moreListener.onMoreModule(_dataList[position])

    }

  }
}