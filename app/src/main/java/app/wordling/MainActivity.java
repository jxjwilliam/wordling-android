package app.wordling;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(246, 248, 251);
    private static final int INK = Color.rgb(24, 37, 54);
    private static final int MUTED = Color.rgb(116, 130, 147);
    private static final int BLUE = Color.rgb(48, 104, 225);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Word> deck = new ArrayList<>();
    private MediaPlayer player;
    private MediaSession mediaSession;
    private boolean playerPrepared;
    private boolean paused;
    private int position;
    private TextView wordView, meaningView, countView, statusView;
    private Button pauseButton;

    private static final class Word {
        final String english, chinese;
        final int audioIndex;
        Word(String english, String chinese, int audioIndex) {
            this.english = english; this.chinese = chinese; this.audioIndex = audioIndex;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        loadVocabulary();
        Collections.shuffle(deck);
        buildUi();
        showCurrent();
        setupMediaSession();
        playCurrent();
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(25), dp(26), dp(25), dp(24));
        page.setBackgroundColor(BG);

        TextView brand = text("WORDLING  ·  词汇听力", 13, BLUE, true);
        brand.setLetterSpacing(.06f);
        page.addView(brand, lp(-1, -2, 0, 0, 0, 26));
        page.addView(text("听一听，记住它。", 28, INK, true), lp(-1, -2, 0, 0, 0, 8));
        page.addView(text("打开就开始，随机听 termvault 词库", 14, MUTED, false), lp(-1, -2, 0, 0, 0, 24));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(18), dp(25), dp(18), dp(25));
        card.setBackground(rounded(Color.WHITE, 24, Color.rgb(229, 235, 243), 1));
        page.addView(card, new LinearLayout.LayoutParams(-1, 0, 1));

        countView = text("随机词汇", 13, BLUE, true);
        countView.setGravity(Gravity.CENTER);
        card.addView(countView, lp(-1, -2, 0, 0, 0, 32));
        wordView = text("customer", 34, INK, true);
        wordView.setGravity(Gravity.CENTER);
        wordView.setMaxLines(2);
        wordView.setAutoSizeTextTypeUniformWithConfiguration(16, 34, 1, TypedValue.COMPLEX_UNIT_SP);
        card.addView(wordView, lp(-1, -2, 0, 0, 0, 12));
        TextView englishHint = text("英文发音", 13, MUTED, false);
        englishHint.setGravity(Gravity.CENTER);
        card.addView(englishHint, lp(-1, -2, 0, 0, 0, 30));

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(235, 239, 245));
        card.addView(divider, lp(-1, 1, 20, 0, 20, 25));
        meaningView = text("客户", 25, BLUE, true);
        meaningView.setGravity(Gravity.CENTER);
        meaningView.setMaxLines(2);
        meaningView.setAutoSizeTextTypeUniformWithConfiguration(14, 25, 1, TypedValue.COMPLEX_UNIT_SP);
        card.addView(meaningView, lp(-1, -2, 0, 0, 0, 8));
        TextView chineseHint = text("中文意思", 13, MUTED, false);
        chineseHint.setGravity(Gravity.CENTER);
        card.addView(chineseHint, lp(-1, -2, 0, 0, 0, 8));

        statusView = text("本地音频 · 自动播放", 13, MUTED, false);
        statusView.setGravity(Gravity.CENTER);
        page.addView(statusView, lp(-1, dp(42), 0, 12, 0, 8));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        pauseButton = button("暂停", true);
        pauseButton.setOnClickListener(v -> togglePause());
        controls.addView(pauseButton, new LinearLayout.LayoutParams(0, dp(50), 1));
        Button replay = button("重听", false);
        replay.setOnClickListener(v -> replayCurrent());
        LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(0, dp(50), 1);
        side.leftMargin = dp(8);
        controls.addView(replay, side);
        Button next = button("下一个", false);
        next.setOnClickListener(v -> nextWord());
        LinearLayout.LayoutParams nextLp = new LinearLayout.LayoutParams(0, dp(50), 1);
        nextLp.leftMargin = dp(8);
        controls.addView(next, nextLp);
        page.addView(controls, lp(-1, dp(50), 0, 0, 0, 8));

        TextView footer = text("共 " + deck.size() + " 条 · termvault 离线词库 · 耳机可控制", 12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, lp(-1, dp(30), 0, 4, 0, 0));
        setContentView(page);
    }

    private void loadVocabulary() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("termvault.txt"), StandardCharsets.UTF_8))) {
            String line;
            int audioIndex = 0;
            while ((line = reader.readLine()) != null) {
                int separator = line.indexOf('—');
                if (separator <= 0) continue;
                String english = line.substring(0, separator).trim();
                String chinese = line.substring(separator + 1).trim();
                if (english.isEmpty() || chinese.isEmpty()) continue;
                deck.add(new Word(english, chinese, audioIndex++));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load bundled termvault.txt", e);
        }
    }

    private void showCurrent() {
        Word word = deck.get(position);
        wordView.setText(word.english);
        meaningView.setText(word.chinese);
        countView.setText("第 " + (position + 1) + " / " + deck.size() + " 组 · 随机顺序");
        updateMediaSession();
    }

    private void setupMediaSession() {
        mediaSession = new MediaSession(this, "WordlingVocabularyPlayer");
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { resumePlayback(); }
            @Override public void onPause() { pausePlayback(); }
            @Override public void onSkipToNext() { nextWord(); }
            @Override public void onSkipToPrevious() { previousWord(); }
            @Override public void onSeekTo(long position) {
                if (player != null && playerPrepared) player.seekTo((int) position);
            }
        });
        mediaSession.setActive(true);
        updateMediaSession();
    }

    private void updateMediaSession() {
        if (mediaSession == null) return;
        Word word = deck.get(position);
        mediaSession.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, word.english)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, word.chinese)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, "词汇听力")
                .build());
        long actions = PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SEEK_TO;
        int state = paused ? PlaybackState.STATE_PAUSED
                : playerPrepared && player != null && player.isPlaying() ? PlaybackState.STATE_PLAYING
                : PlaybackState.STATE_BUFFERING;
        long currentPosition = player != null && playerPrepared ? player.getCurrentPosition() : 0;
        mediaSession.setPlaybackState(new PlaybackState.Builder()
                .setActions(actions)
                .setState(state, currentPosition, 1f)
                .build());
    }

    private void pausePlayback() {
        if (paused) return;
        paused = true;
        handler.removeCallbacksAndMessages(null);
        if (player != null && player.isPlaying()) player.pause();
        pauseButton.setText("继续");
        statusView.setText("已暂停");
        updateMediaSession();
    }

    private void resumePlayback() {
        if (!paused) return;
        paused = false;
        pauseButton.setText("暂停");
        if (player != null && playerPrepared) {
            player.start();
            statusView.setText("正在播放 · 英文，然后中文");
            updateMediaSession();
        } else if (player == null) {
            playCurrent();
        }
    }

    private void previousWord() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        position = position > 0 ? position - 1 : deck.size() - 1;
        paused = false;
        pauseButton.setText("暂停");
        showCurrent();
        playCurrent();
    }

    private void playCurrent() {
        if (paused) return;
        releasePlayer();
        try {
            String asset = String.format(Locale.US, "audio/w%03d.m4a", deck.get(position).audioIndex);
            AssetFileDescriptor descriptor = getAssets().openFd(asset);
            player = new MediaPlayer();
            playerPrepared = false;
            player.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            MediaPlayer current = player;
            player.setOnPreparedListener(mp -> {
                if (current != player) return;
                playerPrepared = true;
                if (paused) return;
                statusView.setText("正在播放 · 英文，然后中文");
                mp.start();
                updateMediaSession();
            });
            player.setOnCompletionListener(mp -> {
                if (current != player || paused) return;
                statusView.setText("自动播放 · 随机词汇");
                handler.postDelayed(() -> { if (current == player && !paused) nextWord(); }, 1500);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                statusView.setText("音频播放失败");
                return true;
            });
            player.prepareAsync();
        } catch (IOException e) {
            statusView.setText("找不到词条音频");
        }
    }

    private void nextWord() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        position++;
        if (position >= deck.size()) {
            int previousAudio = deck.get(deck.size() - 1).audioIndex;
            Collections.shuffle(deck);
            position = 0;
            if (deck.size() > 1 && deck.get(0).audioIndex == previousAudio) Collections.swap(deck, 0, 1);
        }
        paused = false;
        pauseButton.setText("暂停");
        showCurrent();
        playCurrent();
    }

    private void togglePause() {
        if (!paused) {
            paused = true;
            handler.removeCallbacksAndMessages(null);
            if (player != null && player.isPlaying()) player.pause();
            pauseButton.setText("继续");
            statusView.setText("已暂停");
            updateMediaSession();
        } else {
            resumePlayback();
        }
    }

    private void replayCurrent() {
        paused = false;
        pauseButton.setText("暂停");
        handler.removeCallbacksAndMessages(null);
        if (player != null && playerPrepared) {
            player.seekTo(0);
            player.start();
            statusView.setText("正在重听当前词");
            updateMediaSession();
        } else if (player == null) playCurrent();
    }

    private void releasePlayer() {
        if (player != null) {
            try { player.stop(); } catch (IllegalStateException ignored) { }
            player.release();
            player = null;
            playerPrepared = false;
        }
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.WHITE : INK);
        button.setBackground(rounded(primary ? BLUE : Color.rgb(235, 240, 247), 15, Color.TRANSPARENT, 0));
        button.setPadding(dp(5), 0, dp(5), 0);
        return button;
    }

    private GradientDrawable rounded(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (strokeWidth > 0) shape.setStroke(dp(strokeWidth), strokeColor);
        return shape;
    }

    private LinearLayout.LayoutParams lp(int width, int height, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width < 0 ? width : dp(width), height < 0 ? height : dp(height));
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}
