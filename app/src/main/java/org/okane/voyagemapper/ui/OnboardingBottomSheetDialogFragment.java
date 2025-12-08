package org.okane.voyagemapper.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.okane.voyagemapper.R;

public class OnboardingBottomSheetDialogFragment extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.bottom_sheet_onboarding, container, false);

        MaterialButton btnGotIt = v.findViewById(R.id.btnGotIt);
        MaterialButton btnLearnMore = v.findViewById(R.id.btnLearnMore);

        btnGotIt.setOnClickListener(view -> dismiss());

        btnLearnMore.setOnClickListener(view -> {
            dismiss();
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://sokratees9.github.io/VoyageMapper/privacy.html"));
            startActivity(i);
        });

        return v;
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        // Mark as seen so it doesn't show again
        SharedPreferences p = requireActivity()
                .getSharedPreferences("voyage_prefs", android.content.Context.MODE_PRIVATE);
        p.edit().putBoolean("onboarding_seen", true).apply();
    }
}
