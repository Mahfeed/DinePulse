package com.example.dinepulse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.dinepulse.models.MenuItem;
import com.example.dinepulse.repository.CartRepository; // 🛠️ The missing import
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class ModifierBottomSheet extends BottomSheetDialogFragment {

    private MenuItem item;
    private double finalPrice;

    public ModifierBottomSheet(MenuItem item) {
        this.item = item;
        this.finalPrice = item.getPrice();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_modifier_sheet, container, false);

        TextView sheetItemName = view.findViewById(R.id.sheetItemName);
        MaterialButton btnConfirmAdd = view.findViewById(R.id.btnConfirmAdd);
        RadioGroup radioGroupSpice = view.findViewById(R.id.radioGroupSpice);
        CheckBox checkEgg = view.findViewById(R.id.checkEgg);
        CheckBox checkSalad = view.findViewById(R.id.checkSalad);

        sheetItemName.setText(item.getName());
        updatePriceButton(btnConfirmAdd);

        View.OnClickListener priceUpdater = v -> {
            finalPrice = item.getPrice();
            if (checkEgg.isChecked()) finalPrice += 30.0;
            if (checkSalad.isChecked()) finalPrice += 20.0;
            updatePriceButton(btnConfirmAdd);
        };

        checkEgg.setOnClickListener(priceUpdater);
        checkSalad.setOnClickListener(priceUpdater);

        btnConfirmAdd.setOnClickListener(v -> {
            // 🛠️ Generate the notes based on what the guest checked
            String notes = "";
            if (checkEgg.isChecked()) notes += "Add Egg, ";
            if (checkSalad.isChecked()) notes += "Extra Salad";
            if (notes.endsWith(", ")) notes = notes.substring(0, notes.length() - 2);
            if (notes.isEmpty()) notes = "Standard";

            // 🛠️ Pass all three required arguments to the advanced cart
            CartRepository.getInstance().addItemToCart(item, finalPrice, notes);

            Toast.makeText(getContext(), item.getName() + " added to cart", Toast.LENGTH_SHORT).show();
            dismiss();
        });

        return view;
    }

    private void updatePriceButton(MaterialButton btn) {
        btn.setText(String.format("Add to Cart - ৳%.2f", finalPrice));
    }
}