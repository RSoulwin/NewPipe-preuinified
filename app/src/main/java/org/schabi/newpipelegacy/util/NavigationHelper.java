package org.schabi.newpipelegacy.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.schabi.newpipelegacy.MainActivity;
import org.schabi.newpipelegacy.R;
import org.schabi.newpipelegacy.RouterActivity;
import org.schabi.newpipelegacy.about.AboutActivity;
import org.schabi.newpipelegacy.database.feed.model.FeedGroupEntity;
import org.schabi.newpipelegacy.download.DownloadActivity;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipelegacy.fragments.MainFragment;
import org.schabi.newpipelegacy.fragments.detail.VideoDetailFragment;
import org.schabi.newpipelegacy.fragments.list.channel.ChannelFragment;
import org.schabi.newpipelegacy.fragments.list.comments.CommentsFragment;
import org.schabi.newpipelegacy.fragments.list.kiosk.KioskFragment;
import org.schabi.newpipelegacy.fragments.list.playlist.PlaylistFragment;
import org.schabi.newpipelegacy.fragments.list.search.SearchFragment;
import org.schabi.newpipelegacy.local.bookmark.BookmarkFragment;
import org.schabi.newpipelegacy.local.feed.FeedFragment;
import org.schabi.newpipelegacy.local.history.StatisticsPlaylistFragment;
import org.schabi.newpipelegacy.local.playlist.LocalPlaylistFragment;
import org.schabi.newpipelegacy.local.subscription.SubscriptionFragment;
import org.schabi.newpipelegacy.local.subscription.SubscriptionsImportFragment;
import org.schabi.newpipelegacy.player.BackgroundPlayer;
import org.schabi.newpipelegacy.player.BackgroundPlayerActivity;
import org.schabi.newpipelegacy.player.BasePlayer;
import org.schabi.newpipelegacy.player.MainVideoPlayer;
import org.schabi.newpipelegacy.player.PopupVideoPlayer;
import org.schabi.newpipelegacy.player.PopupVideoPlayerActivity;
import org.schabi.newpipelegacy.player.VideoPlayer;
import org.schabi.newpipelegacy.player.playqueue.PlayQueue;
import org.schabi.newpipelegacy.settings.SettingsActivity;

import java.util.ArrayList;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class NavigationHelper {
    public static final String MAIN_FRAGMENT_TAG = "main_fragment_tag";
    public static final String SEARCH_FRAGMENT_TAG = "search_fragment_tag";

    private NavigationHelper() { }

    /*//////////////////////////////////////////////////////////////////////////
    // Players
    //////////////////////////////////////////////////////////////////////////*/

    @NonNull
    public static Intent getPlayerIntent(@NonNull final Context context,
                                         @NonNull final Class targetClazz,
                                         @NonNull final PlayQueue playQueue,
                                         @Nullable final String quality,
                                         final boolean resumePlayback) {
        Intent intent = new Intent(context, targetClazz);

        final String cacheKey = SerializedCache.getInstance().put(playQueue, PlayQueue.class);
        if (cacheKey != null) {
            intent.putExtra(VideoPlayer.PLAY_QUEUE_KEY, cacheKey);
        }
        if (quality != null) {
            intent.putExtra(VideoPlayer.PLAYBACK_QUALITY, quality);
        }
        intent.putExtra(VideoPlayer.RESUME_PLAYBACK, resumePlayback);

        return intent;
    }

    @NonNull
    public static Intent getPlayerIntent(@NonNull final Context context,
                                         @NonNull final Class targetClazz,
                                         @NonNull final PlayQueue playQueue,
                                         final boolean resumePlayback) {
        return getPlayerIntent(context, targetClazz, playQueue, null, resumePlayback);
    }

    @NonNull
    public static Intent getPlayerEnqueueIntent(@NonNull final Context context,
                                                @NonNull final Class targetClazz,
                                                @NonNull final PlayQueue playQueue,
                                                final boolean selectOnAppend,
                                                final boolean resumePlayback) {
        return getPlayerIntent(context, targetClazz, playQueue, resumePlayback)
                .putExtra(BasePlayer.APPEND_ONLY, true)
                .putExtra(BasePlayer.SELECT_ON_APPEND, selectOnAppend);
    }

    @NonNull
    public static Intent getPlayerIntent(@NonNull final Context context,
                                         @NonNull final Class targetClazz,
                                         @NonNull final PlayQueue playQueue,
                                         final int repeatMode, final float playbackSpeed,
                                         final float playbackPitch,
                                         final boolean playbackSkipSilence,
                                         @Nullable final String playbackQuality,
                                         final boolean resumePlayback, final boolean startPaused,
                                         final boolean isMuted) {
        return getPlayerIntent(context, targetClazz, playQueue, playbackQuality, resumePlayback)
                .putExtra(BasePlayer.REPEAT_MODE, repeatMode)
                .putExtra(BasePlayer.START_PAUSED, startPaused)
                .putExtra(BasePlayer.IS_MUTED, isMuted);
    }

    public static void playOnMainPlayer(final Context context,
	                                    final PlayQueue queue,
                                        final boolean resumePlayback) {
        final Intent playerIntent
                = getPlayerIntent(context, MainVideoPlayer.class, queue, resumePlayback);
        playerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(playerIntent);
    }

    public static void playOnPopupPlayer(final Context context,
	                                     final PlayQueue queue,
                                         final boolean resumePlayback) {
        if (!PermissionHelper.isPopupEnabled(context)) {
            PermissionHelper.showPopupEnablementToast(context);
            return;
        }

        Toast.makeText(context, R.string.popup_playing_toast, Toast.LENGTH_SHORT).show();
        startService(context,
                getPlayerIntent(context, PopupVideoPlayer.class, queue, resumePlayback));
    }

    public static void playOnBackgroundPlayer(final Context context,
	                                          final PlayQueue queue,
                                              final boolean resumePlayback) {
        Toast.makeText(context, R.string.background_player_playing_toast, Toast.LENGTH_SHORT)
                .show();
        startService(context,
                getPlayerIntent(context, BackgroundPlayer.class, queue, resumePlayback));
    }

    public static void enqueueOnPopupPlayer(final Context context, final PlayQueue queue,
                                            final boolean resumePlayback) {
        enqueueOnPopupPlayer(context, queue, false, resumePlayback);
    }

    public static void enqueueOnPopupPlayer(final Context context, final PlayQueue queue,
                                            final boolean selectOnAppend,
                                            final boolean resumePlayback) {
        if (!PermissionHelper.isPopupEnabled(context)) {
            PermissionHelper.showPopupEnablementToast(context);
            return;
        }

        Toast.makeText(context, R.string.popup_playing_append, Toast.LENGTH_SHORT).show();
        startService(context, getPlayerEnqueueIntent(context, PopupVideoPlayer.class, queue,
                selectOnAppend, resumePlayback));
    }

    public static void enqueueOnBackgroundPlayer(final Context context, final PlayQueue queue,
                                                 final boolean resumePlayback) {
        enqueueOnBackgroundPlayer(context, queue, false, resumePlayback);
    }

    public static void enqueueOnBackgroundPlayer(final Context context, final PlayQueue queue,
                                                 final boolean selectOnAppend,
                                                 final boolean resumePlayback) {
        Toast.makeText(context, R.string.background_player_append, Toast.LENGTH_SHORT).show();
        startService(context, getPlayerEnqueueIntent(context, BackgroundPlayer.class, queue,
                selectOnAppend, resumePlayback));
    }

    public static void startService(@NonNull final Context context, @NonNull final Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // External Players
    //////////////////////////////////////////////////////////////////////////*/

    public static void playOnExternalAudioPlayer(final Context context, final StreamInfo info) {
        final int index = ListHelper.getDefaultAudioFormat(context, info.getAudioStreams());

        if (index == -1) {
            Toast.makeText(context, R.string.audio_streams_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        AudioStream audioStream = info.getAudioStreams().get(index);
        playOnExternalPlayer(context, info.getName(), info.getUploaderName(), audioStream);
    }

    public static void playOnExternalVideoPlayer(final Context context, final StreamInfo info) {
        ArrayList<VideoStream> videoStreamsList = new ArrayList<>(
                ListHelper.getSortedStreamVideosList(context, info.getVideoStreams(), null, false));
        int index = ListHelper.getDefaultResolutionIndex(context, videoStreamsList);

        if (index == -1) {
            Toast.makeText(context, R.string.video_streams_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        VideoStream videoStream = videoStreamsList.get(index);
        playOnExternalPlayer(context, info.getName(), info.getUploaderName(), videoStream);
    }

    public static void playOnExternalPlayer(final Context context, final String name,
                                            final String artist, final Stream stream) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(stream.getUrl()), stream.getFormat().getMimeType());
        intent.putExtra(Intent.EXTRA_TITLE, name);
        intent.putExtra("title", name);
        intent.putExtra("artist", artist);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        resolveActivityOrAskToInstall(context, intent);
    }

    public static void resolveActivityOrAskToInstall(final Context context, final Intent intent) {
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            if (context instanceof Activity) {
                new AlertDialog.Builder(context)
                        .setMessage(R.string.no_player_found)
                        .setPositiveButton(R.string.install, (dialog, which) -> {
                            Intent i = new Intent();
                            i.setAction(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(context.getString(R.string.fdroid_vlc_url)));
                            context.startActivity(i);
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which)
                                -> Log.i("NavigationHelper", "You unlocked a secret unicorn."))
                        .show();
//                Log.e("NavigationHelper",
//                        "Either no Streaming player for audio was installed, "
//                                + "or something important crashed:");
            } else {
                Toast.makeText(context, R.string.no_player_found_toast, Toast.LENGTH_LONG).show();
            }
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Through FragmentManager
    //////////////////////////////////////////////////////////////////////////*/

    @SuppressLint("CommitTransaction")
    private static FragmentTransaction defaultTransaction(final FragmentManager fragmentManager) {
        return fragmentManager.beginTransaction()
                .setCustomAnimations(R.animator.custom_fade_in, R.animator.custom_fade_out,
                        R.animator.custom_fade_in, R.animator.custom_fade_out);
    }

    public static void gotoMainFragment(final FragmentManager fragmentManager) {
        ImageLoader.getInstance().clearMemoryCache();

        boolean popped = fragmentManager.popBackStackImmediate(MAIN_FRAGMENT_TAG, 0);
        if (!popped) {
            openMainFragment(fragmentManager);
        }
    }

    public static void openMainFragment(final FragmentManager fragmentManager) {
        InfoCache.getInstance().trimCache();

        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new MainFragment())
                .addToBackStack(MAIN_FRAGMENT_TAG)
                .commit();
    }

    public static boolean tryGotoSearchFragment(final FragmentManager fragmentManager) {
        if (MainActivity.DEBUG) {
            for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
                Log.d("NavigationHelper", "tryGoToSearchFragment() [" + i + "]"
                        + " = [" + fragmentManager.getBackStackEntryAt(i) + "]");
            }
        }

        return fragmentManager.popBackStackImmediate(SEARCH_FRAGMENT_TAG, 0);
    }

    public static void openSearchFragment(final FragmentManager fragmentManager,
                                          final int serviceId, final String searchString) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, SearchFragment.getInstance(serviceId, searchString))
                .addToBackStack(SEARCH_FRAGMENT_TAG)
                .commit();
    }

    public static void openVideoDetailFragment(final FragmentManager fragmentManager,
                                               final int serviceId, final String url,
                                               final String title) {
        openVideoDetailFragment(fragmentManager, serviceId, url, title, false);
    }

    public static void openVideoDetailFragment(final FragmentManager fragmentManager,
                                               final int serviceId, final String url,
                                               final String name, final boolean autoPlay) {
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragment_holder);

        if (fragment instanceof VideoDetailFragment && fragment.isVisible()) {
            VideoDetailFragment detailFragment = (VideoDetailFragment) fragment;
            detailFragment.setAutoplay(autoPlay);
            detailFragment.selectAndLoadVideo(serviceId, url, name == null ? "" : name);
            return;
        }

        VideoDetailFragment instance = VideoDetailFragment.getInstance(serviceId, url,
                name == null ? "" : name);
        instance.setAutoplay(autoPlay);

        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, instance)
                .addToBackStack(null)
                .commit();
    }

    public static void openChannelFragment(final FragmentManager fragmentManager,
                                           final int serviceId, final String url,
                                           final String name) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, ChannelFragment.getInstance(serviceId, url,
                        name == null ? "" : name))
                .addToBackStack(null)
                .commit();
    }

    public static void openCommentsFragment(final FragmentManager fragmentManager,
                                            final int serviceId, final String url,
                                            final String name) {
        fragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.switch_service_in, R.anim.switch_service_out)
                .replace(R.id.fragment_holder, CommentsFragment.getInstance(serviceId, url,
                        name == null ? "" : name))
                .addToBackStack(null)
                .commit();
    }

    public static void openPlaylistFragment(final FragmentManager fragmentManager,
                                            final int serviceId, final String url,
                                            final String name) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, PlaylistFragment.getInstance(serviceId, url,
                        name == null ? "" : name))
                .addToBackStack(null)
                .commit();
    }

    public static void openFeedFragment(final FragmentManager fragmentManager) {
        openFeedFragment(fragmentManager, FeedGroupEntity.GROUP_ALL_ID, null);
    }

    public static void openFeedFragment(final FragmentManager fragmentManager, final long groupId,
                                        @Nullable final String groupName) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, FeedFragment.newInstance(groupId, groupName))
                .addToBackStack(null)
                .commit();
    }

    public static void openBookmarksFragment(final FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new BookmarkFragment())
                .addToBackStack(null)
                .commit();
    }

    public static void openSubscriptionFragment(final FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new SubscriptionFragment())
                .addToBackStack(null)
                .commit();
    }

    public static void openKioskFragment(final FragmentManager fragmentManager, final int serviceId,
                                         final String kioskId) throws ExtractionException {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, KioskFragment.getInstance(serviceId, kioskId))
                .addToBackStack(null)
                .commit();
    }

    public static void openLocalPlaylistFragment(final FragmentManager fragmentManager,
                                                 final long playlistId, final String name) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, LocalPlaylistFragment.getInstance(playlistId,
                        name == null ? "" : name))
                .addToBackStack(null)
                .commit();
    }

    public static void openStatisticFragment(final FragmentManager fragmentManager) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, new StatisticsPlaylistFragment())
                .addToBackStack(null)
                .commit();
    }

    public static void openSubscriptionsImportFragment(final FragmentManager fragmentManager,
                                                       final int serviceId) {
        defaultTransaction(fragmentManager)
                .replace(R.id.fragment_holder, SubscriptionsImportFragment.getInstance(serviceId))
                .addToBackStack(null)
                .commit();
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Through Intents
    //////////////////////////////////////////////////////////////////////////*/

    public static void openSearch(final Context context, final int serviceId,
                                  final String searchString) {
        Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.putExtra(Constants.KEY_SERVICE_ID, serviceId);
        mIntent.putExtra(Constants.KEY_SEARCH_STRING, searchString);
        mIntent.putExtra(Constants.KEY_OPEN_SEARCH, true);
        context.startActivity(mIntent);
    }

    public static void openChannel(final Context context, final int serviceId, final String url) {
        openChannel(context, serviceId, url, null);
    }

    public static void openChannel(final Context context, final int serviceId,
                                   final String url, final String name) {
        Intent openIntent = getOpenIntent(context, url, serviceId,
                StreamingService.LinkType.CHANNEL);
        if (name != null && !name.isEmpty()) {
            openIntent.putExtra(Constants.KEY_TITLE, name);
        }
        context.startActivity(openIntent);
    }

    public static void openVideoDetail(final Context context, final int serviceId,
                                       final String url) {
        openVideoDetail(context, serviceId, url, null);
    }

    public static void openVideoDetail(final Context context, final int serviceId,
                                       final String url, final String title) {
        Intent openIntent = getOpenIntent(context, url, serviceId,
                StreamingService.LinkType.STREAM);
        if (title != null && !title.isEmpty()) {
            openIntent.putExtra(Constants.KEY_TITLE, title);
        }
        context.startActivity(openIntent);
    }

    public static void openMainActivity(final Context context) {
        Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(mIntent);
    }

    public static void openRouterActivity(final Context context, final String url) {
        Intent mIntent = new Intent(context, RouterActivity.class);
        mIntent.setData(Uri.parse(url));
        mIntent.putExtra(RouterActivity.INTERNAL_ROUTE_KEY, true);
        context.startActivity(mIntent);
    }

    public static void openBackgroundPlayer(final Context context) {
        final Intent intent = new Intent(context, BackgroundPlayerActivity.class);
        context.startActivity(intent);
    }

    public static void openAbout(final Context context) {
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    public static void openSettings(final Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    public static boolean openDownloads(final Activity activity) {
        if (!PermissionHelper.checkStoragePermissions(
                activity, PermissionHelper.DOWNLOADS_REQUEST_CODE)) {
            return false;
        }
        Intent intent = new Intent(activity, DownloadActivity.class);
        activity.startActivity(intent);
        return true;
    }

    public static Intent getBackgroundPlayerActivityIntent(final Context context) {
        return getServicePlayerActivityIntent(context, BackgroundPlayerActivity.class);
    }

    public static Intent getPopupPlayerActivityIntent(final Context context) {
        return getServicePlayerActivityIntent(context, PopupVideoPlayerActivity.class);
    }

    private static Intent getServicePlayerActivityIntent(final Context context,
                                                         final Class activityClass) {
        Intent intent = new Intent(context, activityClass);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }
    /*//////////////////////////////////////////////////////////////////////////
    // Link handling
    //////////////////////////////////////////////////////////////////////////*/

    private static Intent getOpenIntent(final Context context, final String url,
                                        final int serviceId, final StreamingService.LinkType type) {
        Intent mIntent = new Intent(context, MainActivity.class);
        mIntent.putExtra(Constants.KEY_SERVICE_ID, serviceId);
        mIntent.putExtra(Constants.KEY_URL, url);
        mIntent.putExtra(Constants.KEY_LINK_TYPE, type);
        return mIntent;
    }

    public static Intent getIntentByLink(final Context context, final String url)
            throws ExtractionException {
        return getIntentByLink(context, NewPipe.getServiceByUrl(url), url);
    }

    public static Intent getIntentByLink(final Context context, final StreamingService service,
                                         final String url) throws ExtractionException {
        StreamingService.LinkType linkType = service.getLinkTypeByUrl(url);

        if (linkType == StreamingService.LinkType.NONE) {
            throw new ExtractionException("Url not known to service. service=" + service
                    + " url=" + url);
        }

        Intent rIntent = getOpenIntent(context, url, service.getServiceId(), linkType);

        if (linkType == StreamingService.LinkType.STREAM) {
            rIntent.putExtra(VideoDetailFragment.AUTO_PLAY,
                    PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                            context.getString(R.string.autoplay_through_intent_key), false));
        }

        return rIntent;
    }

    private static Uri openMarketUrl(final String packageName) {
        return Uri.parse("market://details")
                .buildUpon()
                .appendQueryParameter("id", packageName)
                .build();
    }

    private static Uri getGooglePlayUrl(final String packageName) {
        return Uri.parse("https://play.google.com/store/apps/details")
                .buildUpon()
                .appendQueryParameter("id", packageName)
                .build();
    }

    private static void installApp(final Context context, final String packageName) {
        try {
            // Try market:// scheme
            context.startActivity(new Intent(Intent.ACTION_VIEW, openMarketUrl(packageName)));
        } catch (ActivityNotFoundException e) {
            // Fall back to google play URL (don't worry F-Droid can handle it :)
            context.startActivity(new Intent(Intent.ACTION_VIEW, getGooglePlayUrl(packageName)));
        }
    }

    /**
     * Start an activity to install Kore.
     *
     * @param context the context
     */
    public static void installKore(final Context context) {
        installApp(context, context.getString(R.string.kore_package));
    }

    /**
     * Start Kore app to show a video on Kodi.
     * <p>
     * For a list of supported urls see the
     * <a href="https://github.com/xbmc/Kore/blob/master/app/src/main/AndroidManifest.xml">
     * Kore source code
     * </a>.
     *
     * @param context  the context to use
     * @param videoURL the url to the video
     */
    public static void playWithKore(final Context context, final Uri videoURL) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setPackage(context.getString(R.string.kore_package));
        intent.setData(videoURL);
        context.startActivity(intent);
    }
}
