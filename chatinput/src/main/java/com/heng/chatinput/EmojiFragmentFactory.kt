package com.heng.chatinput

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.TextView
import com.heng.chatinput.utils.Utils
import com.vdurmont.emoji.EmojiManager

interface EmojiFragmentFactory {
  fun create(emojiListener: EmojiListener): Fragment
}

internal class DefaultEmojiFragmentFactory : EmojiFragmentFactory {
  override fun create(emojiListener: EmojiListener): Fragment {
    return EmojiMainFragment().apply {
      this.emojiListener = emojiListener
    }
  }
}

interface EmojiListener {
  fun inputEmoji(emoji: CharSequence)
  fun delete()
}

/**
 * 键盘表情的Fragment
 */
class EmojiMainFragment : Fragment() {

  private lateinit var viewPager: ViewPager
  private lateinit var itemDelete: FrameLayout
  private lateinit var lLayoutTab: LinearLayout
  var emojiListener: EmojiListener? = null
  private val tagViewList = mutableListOf<View>()
  private val fragmentList = mutableListOf<Fragment>()

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_main_emoji, container, false)
    viewPager = view.findViewById(R.id.vp)
    itemDelete = view.findViewById(R.id.item_delete)
    lLayoutTab = view.findViewById(R.id.llayout_tab)
    return view
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    setBottomTab()
    viewPager.adapter = object : FragmentPagerAdapter(childFragmentManager) {
      override fun getCount() = fragmentList.size
      override fun getItem(position: Int) = fragmentList[position]
    }
    selectTab(0)
  }

  //初始化底部tab
  private fun setBottomTab() {
    val screenWidth = Utils.getScreenWidthPixels(activity)
    val itemWidth = screenWidth / 6
    itemDelete.layoutParams.width = itemWidth
    itemDelete.setOnClickListener { emojiListener?.delete() }
    val view = LayoutInflater.from(context)
        .inflate(R.layout.item_emoji_tab, null)
    view.layoutParams = LinearLayout.LayoutParams(itemWidth, ViewGroup.LayoutParams.MATCH_PARENT)
    view.tag = 0
    view.setOnClickListener(tagClickListener)
    lLayoutTab.addView(view)
    tagViewList.add(view)
    fragmentList.add(
        EmojiOneFragment().apply { this.emojiListener = this@EmojiMainFragment.emojiListener })
  }

  //底部tab点击事件
  private val tagClickListener = View.OnClickListener { v ->
    val position = v?.tag as Int
    selectTab(position)
  }

  private fun selectTab(position: Int) {
    viewPager.currentItem = position
    tagViewList.forEachIndexed { index, view ->
      view.setBackgroundColor(
          if (index == position) Color.parseColor("#70a3a4a7")
          else Color.TRANSPARENT
      )
    }
  }

  //第一页emoji的fragment
  internal class EmojiOneFragment : Fragment() {
    var emojiListener: EmojiListener? = null
    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View? {
      return inflater.inflate(R.layout.item_emoji, container, false)
    }

    override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
    ) {
      super.onViewCreated(view, savedInstanceState)
      val gridView = view.findViewById<GridView>(R.id.gridview)
      val emojiList = EmojiManager.getAll()
          .toMutableList()
      gridView.adapter = object : BaseAdapter() {
        override fun getView(
          position: Int,
          convertView: View?,
          parent: ViewGroup?
        ): TextView? {
          var textView: TextView? = null
          if (convertView == null) {
            textView = LayoutInflater.from(context).inflate(R.layout.view_emoji, null) as TextView
            textView.layoutParams = AbsListView.LayoutParams(
                Utils.getScreenWidthPixels(activity) / 7,
                (EmotionKeyboard.getKeyBoardHeight() - Utils.dip2px(context, 40f)) / 4
            )

          } else {
            textView = convertView as TextView
          }
          textView.setOnClickListener { emojiListener?.inputEmoji(getItem(position).unicode) }
          return textView.apply { this.text = getItem(position).unicode }
        }

        override fun getItem(position: Int) = emojiList[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun getCount() = emojiList.size
      }
    }
  }

}