package com.example.rocketdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.rocketdemo.model.Bin;
import com.example.rocketdemo.util.CardNumberUtils;
import com.example.rocketdemo.util.UIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class MainActivity extends Activity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final float CARD_IMAGE_HEIGHT_MULTIPLIER = 0.6f;
    private static final long ANIMATION_DURATION = 500;

    private View mainContent;
    private View mainContainer;
    private View mainInfoEmpty;
    private View mainInfoFilled;
    private TextView infoText;
    private EditText inputCardNumber;
    private EditText inputValidThrough;
    private EditText inputCvv;
    private EditText inputAmount;
    private ImageView cardImage;
    private ImageView cardImageSecondary;
    private ImageView inputArrow;
    private HorizontalScrollView mainContainerScroll;

    private String previousBinNumber = "";

    private class BinTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            final String binNumber = params[0];
            if (TextUtils.isEmpty(binNumber) || (binNumber.length() < CardNumberUtils.BIN_MIN_LENGTH) ||
                    binNumber.substring(0, CardNumberUtils.BIN_MIN_LENGTH).equals(previousBinNumber)) {
                return null;
            }
            previousBinNumber = binNumber.substring(0, CardNumberUtils.BIN_MIN_LENGTH);

            String jsonResponse;
            try {
                final HttpClient httpClient = new DefaultHttpClient();
                final HttpGet request = new HttpGet(new URI(String.format("https://rocketbank.ru/bins/%s/from", binNumber)));
                final HttpResponse response = httpClient.execute(request);

                final StringBuilder stringBuilder = new StringBuilder();
                final Reader responseReader = new InputStreamReader(response.getEntity().getContent());
                final char[] buf = new char[1024];
                while (responseReader.read(buf, 0, buf.length) > 0) {
                    stringBuilder.append(buf);
                }

                jsonResponse = stringBuilder.toString();
            } catch (URISyntaxException e) {
                Log.e(LOG_TAG, "BinTask", e);
                return null;
            } catch (IOException e) {
                Log.e(LOG_TAG, "BinTask", e);
                return null;
            }

            final Bin bin;
            try {
                bin = new Bin(new JSONObject(jsonResponse));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "BinTask", e);
                return null;
            }

            UIUtils.runInUiThread(new Runnable() {
                @Override
                public void run() {
                    switchTitles(true);

                    if (bin.isWorking()) {
                        mainContent.setBackgroundColor(getResources().getColor(R.color.background_valid));
                        inputCardNumber.setTextColor(getResources().getColor(R.color.card_number_valid));
                    } else {
                        mainContent.setBackgroundColor(getResources().getColor(R.color.background_invalid));
                        inputCardNumber.setTextColor(getResources().getColor(R.color.card_number_invalid));
                    }

                    infoText.setText(bin.getHint());
                    cardImageSecondary.setVisibility(View.VISIBLE);
                    new ExternalImageTask(cardImage).execute(bin.getImageUrl());
                    new ExternalImageTask(cardImageSecondary).execute(bin.getLogoUrl());
                }
            });

            return null;
        }
    }

    private class ExternalImageTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imageView;

        public ExternalImageTask(final ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            final String url = params[0];
            Bitmap bitmap = null;

            try {
                final InputStream inputStream = new URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, "ExternalImageTask", e);
            } catch (IOException e) {
                Log.e(LOG_TAG, "ExternalImageTask", e);
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(final Bitmap result) {
            UIUtils.runInUiThread(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageBitmap(result);
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.main_top_text)).setText(Html.fromHtml(getString(R.string.title)));

        mainContent = findViewById(R.id.main_content);
        mainContainer = findViewById(R.id.main_container);
        mainInfoEmpty = findViewById(R.id.main_info_empty);
        mainInfoFilled = findViewById(R.id.main_info_filled);
        inputCardNumber = (EditText) mainContainer.findViewById(R.id.main_info_card_number);
        inputValidThrough = (EditText) mainContainer.findViewById(R.id.main_info_valid_through);
        inputCvv = (EditText) mainContainer.findViewById(R.id.main_info_cvv);
        inputAmount = (EditText) mainContainer.findViewById(R.id.main_info_amount);
        cardImage = (ImageView) mainContainer.findViewById(R.id.main_info_image);
        cardImageSecondary = (ImageView) mainContainer.findViewById(R.id.main_info_image_secondary);
        infoText = (TextView) findViewById(R.id.main_info_text);
        inputArrow = (ImageView) mainContainer.findViewById(R.id.main_info_arrow);
        mainContainerScroll = (HorizontalScrollView) findViewById(R.id.main_container_scroll);

        mainContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mainContainer.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                final Drawable cardImageDrawable = cardImage.getDrawable();
                final int cardImageHeight = (int) (inputCardNumber.getHeight() * CARD_IMAGE_HEIGHT_MULTIPLIER);
                final int cardImageWidth = (int) (cardImageHeight *
                        ((float) cardImageDrawable.getIntrinsicWidth() / cardImageDrawable.getIntrinsicHeight()));
                UIUtils.setWidthHeight(cardImage, cardImageWidth, cardImageHeight);
                UIUtils.setWidthHeight(cardImageSecondary, cardImageWidth, cardImageHeight);
                UIUtils.setWidthHeight(inputArrow, cardImageHeight, cardImageHeight);
                inputCardNumber.setHint(R.string.card_number_hint);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mainContainerScroll.scrollTo(0, 0);
                    }
                }, 100);
            }
        });

        inputCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean isChanging = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isChanging) {
                    return;
                }

                isChanging = true;
                s.replace(0, s.length(), CardNumberUtils.getFormattedCardNumber(s.toString()));
                if (s.length() > CardNumberUtils.MAX_LENGTH_FORMATTED) {
                    s.delete(CardNumberUtils.MAX_LENGTH_FORMATTED, s.length());
                }
                isChanging = false;

                final String rawCardNumber = CardNumberUtils.getRawCardNumber(s.toString());
                if (TextUtils.isEmpty(rawCardNumber) || (rawCardNumber.length() < CardNumberUtils.BIN_MIN_LENGTH)) {
                    switchTitles(false);
                    mainContent.setBackgroundColor(getResources().getColor(R.color.background_default));
                    inputCardNumber.setTextColor(getResources().getColor(R.color.card_number_valid));
                    cardImage.setImageResource(R.drawable.empty_card);
                    cardImageSecondary.setVisibility(View.GONE);
                } else if (rawCardNumber.length() >= CardNumberUtils.MAX_LENGTH) {
                    if (CardNumberUtils.isLuhnValid(rawCardNumber)) {
                        advanceFocus(inputValidThrough);
                    } else {
                        switchTitles(true);
                        inputCardNumber.setTextColor(getResources().getColor(R.color.card_number_invalid));
                        infoText.setText(R.string.card_number_incorrect);
                    }
                } else if (rawCardNumber.length() >= CardNumberUtils.BIN_MIN_LENGTH) {
                    new BinTask().execute(rawCardNumber);
                }
            }
        });

        inputCardNumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mainContainerScroll.scrollTo(0, 0);
                }
            }
        });

        inputValidThrough.addTextChangedListener(new TextWatcher() {
            private boolean isChanging = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isChanging) {
                    return;
                }

                isChanging = true;
                final boolean isFilled = (s.length() >= 5);
                if (isFilled) {
                    s.replace(0, s.length(), s.subSequence(0, 5));
                } else {
                    final String raw = s.toString().replace("/", "");
                    if (raw.length() > 2) {
                        s.replace(0, s.length(), raw.substring(0, 2) + "/" + raw.substring(2));
                    } else {
                        s.replace(0, s.length(), raw);
                    }
                }
                isChanging = false;

                if (isFilled) {
                    advanceFocus(inputCvv);
                }
            }
        });

        inputCvv.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 3) {
                    advanceFocus(inputAmount);
                }
            }
        });

        mainContainerScroll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        inputArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.dialog_title)
                        .setMessage(R.string.dialog_text)
                        .setIcon(R.drawable.ic_app)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });
    }

    private void advanceFocus(final View view) {
        final int start = mainContainerScroll.getScrollX();
        final int end = (view == inputAmount) ? mainContainer.getWidth() : view.getRight() - mainContainerScroll.getWidth();

        final Animation scroll = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                mainContainerScroll.scrollTo((int) (start + (end - start) * interpolatedTime), 0);
            }
        };
        scroll.setDuration(ANIMATION_DURATION);
        scroll.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        mainContainerScroll.startAnimation(scroll);
    }

    private void switchTitles(final boolean isFilled) {
        mainInfoEmpty.setVisibility(isFilled ? View.INVISIBLE : View.VISIBLE);
        mainInfoFilled.setVisibility(isFilled ? View.VISIBLE : View.INVISIBLE);
    }

}
