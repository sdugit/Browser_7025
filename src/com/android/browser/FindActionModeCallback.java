/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.browser;

import android.content.Context;
import android.content.res.Resources;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.TextWatcher;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

class FindActionModeCallback implements ActionMode.Callback, TextWatcher,
        View.OnLongClickListener {
    private View mCustomView;
    private EditText mEditText;
    private TextView mMatches;
    private WebView mWebView;
    private InputMethodManager mInput;
    private Resources mResources;
    private boolean mMatchesFound;
    private int mNumberOfMatches;
    private BrowserActivity mBrowserActivity;

    FindActionModeCallback(BrowserActivity context) {
        mCustomView = LayoutInflater.from(context).inflate(
                R.layout.browser_find, null);
        mEditText = (EditText) mCustomView.findViewById(R.id.edit);
        // Override long click so that select ActionMode is not opened, which
        // would exit find ActionMode.
        mEditText.setOnLongClickListener(this);
        Spannable span = (Spannable) mEditText.getText();
        int length = span.length();
        span.setSpan(this, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        mMatches = (TextView) mCustomView.findViewById(R.id.matches);
        mInput = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mResources = context.getResources();
        mBrowserActivity = context;
    }

    /*
     * Place text in the text field so it can be searched for.  Need to press
     * the find next or find previous button to find all of the matches.
     */
    void setText(String text) {
        mEditText.setText(text);
        Spannable span = (Spannable) mEditText.getText();
        int length = span.length();
        // Ideally, we would like to set the selection to the whole field,
        // but this brings up the Text selection CAB, which dismisses this
        // one.
        Selection.setSelection(span, length, length);
        // Necessary each time we set the text, so that this will watch
        // changes to it.
        span.setSpan(this, 0, length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        mMatchesFound = false;
    }

    /*
     * Set the WebView to search.  Must be non null, and set before calling
     * startActionMode.
     */
    void setWebView(WebView webView) {
        if (null == webView) {
            throw new AssertionError("WebView supplied to "
                    + "FindActionModeCallback cannot be null");
        }
        mWebView = webView;
    }

    /*
     * Move the highlight to the next match.
     * @param next If true, find the next match further down in the document.
     *             If false, find the previous match, up in the document.
     */
    private void findNext(boolean next) {
        if (mWebView == null) {
            throw new AssertionError(
                    "No WebView for FindActionModeCallback::findNext");
        }
        mWebView.findNext(next);
    }

    /*
     * Highlight all the instances of the string from mEditText in mWebView.
     */
    private void findAll() {
        if (mWebView == null) {
            throw new AssertionError(
                    "No WebView for FindActionModeCallback::findAll");
        }
        CharSequence find = mEditText.getText();
        if (0 == find.length()) {
            mWebView.clearMatches();
            mMatches.setVisibility(View.INVISIBLE);
            mMatchesFound = false;
        } else {
            mMatchesFound = true;
            mMatches.setVisibility(View.VISIBLE);
            mNumberOfMatches = mWebView.findAll(find.toString());
            if (0 == mNumberOfMatches) {
                mMatches.setText(mResources.getString(R.string.no_matches));
            } else {
                updateMatchesString();
            }
        }
    }

    /*
     * Update the string which tells the user how many matches were found, and
     * which match is currently highlighted.
     */
    private void updateMatchesString() {
        String template = mResources.getQuantityString(R.plurals.matches_found,
                mNumberOfMatches, mWebView.findIndex() + 1, mNumberOfMatches);

        mMatches.setText(template);
    }

    // OnLongClickListener implementation

    @Override
    public boolean onLongClick(View v) { return true; }

    // ActionMode.Callback implementation

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.setCustomView(mCustomView);
        mode.getMenuInflater().inflate(R.menu.find, menu);
        // Ideally, we would like to preserve the old find text, but it
        // brings up the Text selection CAB, and therefore dismisses
        // find
        setText("");
        mMatches.setVisibility(View.INVISIBLE);
        mMatchesFound = false;
        mMatches.setText("0");
        mEditText.requestFocus();
        mInput.showSoftInput(mEditText, 0);
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mBrowserActivity.onEndActionMode();
        mWebView.notifyFindDialogDismissed();
        mInput.hideSoftInputFromWindow(mWebView.getWindowToken(), 0);
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (!mMatchesFound) {
            findAll();
            return true;
        }
        switch(item.getItemId()) {
            case R.id.find_prev:
                findNext(false);
                break;
            case R.id.find_next:
                findNext(true);
                break;
            default:
                return false;
        }
        updateMatchesString();
        return true;
    }

    // TextWatcher methods

    @Override
    public void beforeTextChanged(CharSequence s,
                                  int start,
                                  int count,
                                  int after) {
    }

    @Override
    public void onTextChanged(CharSequence s,
                              int start,
                              int before,
                              int count) {
        findAll();
    }

    @Override
    public void afterTextChanged(Editable s) { }

}