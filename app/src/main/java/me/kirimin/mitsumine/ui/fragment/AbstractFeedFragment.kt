package me.kirimin.mitsumine.ui.fragment


import me.kirimin.mitsumine.R
import me.kirimin.mitsumine.db.FeedDAO
import me.kirimin.mitsumine.model.Feed
import me.kirimin.mitsumine.network.RequestQueueSingleton
import me.kirimin.mitsumine.ui.activity.EntryInfoActivity
import me.kirimin.mitsumine.ui.adapter.FeedAdapter
import me.kirimin.mitsumine.ui.adapter.FeedAdapter.FeedAdapterListener

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView

import kotlinx.android.synthetic.fragment_feed.view.*

public abstract class AbstractFeedFragment : Fragment(), FeedAdapterListener, SwipeRefreshLayout.OnRefreshListener {

    private var mAdapter: FeedAdapter? = null

    abstract fun requestFeed()

    abstract fun isUseReadLater(): Boolean

    abstract fun isUseRead(): Boolean

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = inflater.inflate(R.layout.fragment_feed, container, false)
        rootView.swipeLayout.setColorSchemeResources(R.color.blue, R.color.orange)
        rootView.swipeLayout.setOnRefreshListener(this)
        rootView.swipeLayout.setProgressViewOffset(false, 0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, getResources().getDisplayMetrics()).toInt())
        mAdapter = FeedAdapter(getActivity().getApplicationContext(), this, isUseReadLater(), isUseRead())
        rootView.feedListView.setAdapter(mAdapter)
        return rootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super<Fragment>.onActivityCreated(savedInstanceState)
        requestFeed()
    }

    override fun onRefresh() {
        reloadFeed()
    }

    override fun onFeedClick(view: View) {
        val feed = view.getTag() as Feed
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(feed.linkUrl)))
    }

    override fun onFeedLongClick(view: View) {
        val feed = view.getTag() as Feed
        val pref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())
        if (pref.getBoolean(getString(R.string.key_use_browser_to_comment_list), false)) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(feed.entryLinkUrl)))
        } else {
            val intent = Intent(getActivity(), javaClass<EntryInfoActivity>())
            intent.putExtras(EntryInfoActivity.buildBundle(feed.linkUrl))
            startActivity(intent)
        }
    }

    override fun onFeedLeftSlide(view: View) {
        val feed = view.getTag() as Feed
        feed.type = Feed.TYPE_READ
        FeedDAO.save(feed)
        mAdapter!!.remove(feed)
    }

    override fun onFeedRightSlide(view: View) {
        val feed = view.getTag() as Feed
        feed.type = Feed.TYPE_READ_LATER
        FeedDAO.save(feed)
        mAdapter!!.remove(feed)
    }

    override fun onFeedShareClick(view: View) {
        val pref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())
        if (pref.getBoolean(getString(R.string.key_is_share_with_title), false)) {
            val intent = buildShareUrlWithTitleIntent(view.getTag() as Feed)
            startActivity(Intent.createChooser(intent, getString(R.string.feed_share_url_with_title)))
        } else {
            val intent = buildShareUrlIntent(view.getTag() as Feed)
            startActivity(Intent.createChooser(intent, getString(R.string.feed_share_url)))
        }
    }

    override fun onFeedShareLongClick(view: View) {
        val pref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())
        if (!pref.getBoolean(getString(R.string.key_is_share_with_title), false)) {
            val intent = buildShareUrlWithTitleIntent(view.getTag() as Feed)
            startActivity(Intent.createChooser(intent, getString(R.string.feed_share_url_with_title)))
        } else {
            val intent = buildShareUrlIntent(view.getTag() as Feed)
            startActivity(Intent.createChooser(intent, getString(R.string.feed_share_url)))
        }
    }

    protected fun clearFeed() {
        mAdapter!!.clear()
    }

    protected fun setFeed(feedList: List<Feed>) {
        mAdapter!!.addAll(feedList)
    }

    protected fun showRefreshing() {
        getView().swipeLayout.setRefreshing(true)
    }

    protected fun dismissRefreshing() {
        getView().swipeLayout.setRefreshing(false)
    }

    private fun reloadFeed() {
        if (getActivity() != null) {
            RequestQueueSingleton.get(getActivity()).getCache().clear()
        }
        mAdapter!!.clear()
        requestFeed()
    }

    private fun buildShareUrlIntent(feed: Feed): Intent {
        val share = Intent(android.content.Intent.ACTION_SEND)
        share.setType("text/plain")
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        share.putExtra(Intent.EXTRA_SUBJECT, feed.title)
        share.putExtra(Intent.EXTRA_TEXT, feed.linkUrl)

        return share
    }

    private fun buildShareUrlWithTitleIntent(feed: Feed): Intent {
        val share = Intent(android.content.Intent.ACTION_SEND)
        share.setType("text/plain")
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        share.putExtra(Intent.EXTRA_TEXT, feed.title + " " + feed.linkUrl)

        return share
    }
}
