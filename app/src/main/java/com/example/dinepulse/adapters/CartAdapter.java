package com.example.dinepulse.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.dinepulse.R;
import com.example.dinepulse.models.CartItem;
import com.example.dinepulse.repository.CartManager;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartList;

    public CartAdapter(List<CartItem> cartList) {
        this.cartList = cartList;
    }

    public void updateList(List<CartItem> newList) {
        this.cartList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartList.get(position);

        holder.tvItemName.setText(cartItem.getMenuItem().getName());
        holder.tvItemPrice.setText(String.format("৳%.2f", cartItem.getMenuItem().getPrice()));
        holder.tvQuantity.setText(String.valueOf(cartItem.getQuantity()));

        // The Listener Pattern for Granular Quantity Control
        holder.btnPlus.setOnClickListener(v -> CartManager.getInstance().addToCart(cartItem.getMenuItem()));
        holder.btnMinus.setOnClickListener(v -> CartManager.getInstance().decrementItem(cartItem.getMenuItem()));

        // Remove button (red text like in reference image)
        holder.tvRemove.setOnClickListener(v -> CartManager.getInstance().removeItemCompletely(cartItem.getMenuItem()));
    }

    @Override
    public int getItemCount() {
        return cartList != null ? cartList.size() : 0;
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemPrice, tvQuantity, tvRemove;
        TextView btnPlus, btnMinus; // FIXED: Now strictly matches the XML TextViews

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvCartItemName);
            tvItemPrice = itemView.findViewById(R.id.tvCartItemPrice);
            tvQuantity = itemView.findViewById(R.id.tvCartItemQuantity);
            tvRemove = itemView.findViewById(R.id.tvCartItemRemove);
            btnPlus = itemView.findViewById(R.id.btnCartPlus);
            btnMinus = itemView.findViewById(R.id.btnCartMinus);
        }
    }
}