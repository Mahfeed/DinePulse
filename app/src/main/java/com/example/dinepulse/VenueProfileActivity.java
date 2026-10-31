package com.example.dinepulse;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.dinepulse.models.MenuItem;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VenueProfileActivity extends AppCompatActivity {

    private ImageView ivCoverPhoto, ivVenueLogo;
    private TextView tvVenueProfileName, tvVenueBio, tvTableBadge, tvRatingBadge, tvWifiDetails, tvPromoHeadline, tvPromoSubtext, tvOperatingHours;
    private MaterialCardView cardWifi, cardPromo;
    private RecyclerView recyclerTrending;

    private static final String DEFAULT_COVER = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=1000&q=80";
    private static final String DEFAULT_LOGO = "https://images.unsplash.com/photo-1583338917451-face2751d8d5?w=500&q=80";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_venue_profile);

        ivCoverPhoto = findViewById(R.id.ivCoverPhoto);
        ivVenueLogo = findViewById(R.id.ivVenueLogo);
        tvVenueProfileName = findViewById(R.id.tvVenueProfileName);
        tvVenueBio = findViewById(R.id.tvVenueBio);
        tvTableBadge = findViewById(R.id.tvTableBadge);
        tvRatingBadge = findViewById(R.id.tvRatingBadge);
        tvWifiDetails = findViewById(R.id.tvWifiDetails);
        tvPromoHeadline = findViewById(R.id.tvPromoHeadline);
        tvPromoSubtext = findViewById(R.id.tvPromoSubtext);
        tvOperatingHours = findViewById(R.id.tvOperatingHours);
        cardWifi = findViewById(R.id.cardWifi);
        cardPromo = findViewById(R.id.cardPromo);
        recyclerTrending = findViewById(R.id.recyclerTrending);

        recyclerTrending.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        SharedPreferences prefs = getSharedPreferences("DinePulsePrefs", MODE_PRIVATE);
        String currentVenueId = prefs.getString("CURRENT_VENUE", "venue_alpha");
        String currentTableId = prefs.getString("CURRENT_TABLE", "table_00");

        tvTableBadge.setText("📍 Seated at " + currentTableId.replace("_", " ").toUpperCase());

        loadVenueSettings(currentVenueId);
        loadTrendingItems();

        findViewById(R.id.btnExploreMenu).setOnClickListener(v -> {
            startActivity(new Intent(VenueProfileActivity.this, GuestMenuActivity.class));
            finish();
        });
    }

    private void loadVenueSettings(String venueId) {
        FirebaseFirestore.getInstance().collection("venues").document(venueId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String venueName = doc.getString("venueName");
                        String venueBio = doc.getString("venueBio");
                        String ratingText = doc.getString("ratingText");
                        String coverUrl = doc.getString("coverUrl");
                        String logoUrl = doc.getString("logoUrl");
                        Boolean promoEnabled = doc.getBoolean("promoEnabled");
                        String promoHeadline = doc.getString("promoHeadline");
                        String promoSubtext = doc.getString("promoSubtext");
                        String wifiSSID = doc.getString("wifiSSID");
                        String wifiPass = doc.getString("wifiPass");
                        String hours = doc.getString("operatingHours");

                        tvVenueProfileName.setText(venueName != null && !venueName.isEmpty() ? venueName : venueId.toUpperCase());

                        if (venueBio != null && !venueBio.isEmpty()) {
                            tvVenueBio.setText(venueBio);
                            tvVenueBio.setVisibility(View.VISIBLE);
                        } else {
                            tvVenueBio.setVisibility(View.GONE);
                        }

                        if (ratingText != null && !ratingText.isEmpty()) {
                            tvRatingBadge.setText("⭐ " + ratingText);
                            tvRatingBadge.setVisibility(View.VISIBLE);
                        } else {
                            tvRatingBadge.setVisibility(View.GONE);
                        }

                        if (wifiSSID != null && !wifiSSID.trim().isEmpty() && wifiPass != null && !wifiPass.trim().isEmpty()) {
                            tvWifiDetails.setText("Network: " + wifiSSID + "\nPassword: " + wifiPass);
                            cardWifi.setVisibility(View.VISIBLE);
                        } else {
                            cardWifi.setVisibility(View.GONE);
                        }

                        if (promoEnabled != null && promoEnabled) {
                            tvPromoHeadline.setText(promoHeadline != null && !promoHeadline.isEmpty() ? promoHeadline : "🎁 Promotions Available");
                            tvPromoSubtext.setText(promoSubtext != null && !promoSubtext.isEmpty() ? promoSubtext : "✨ SPECIAL OFFER");
                            cardPromo.setVisibility(View.VISIBLE);
                        } else {
                            cardPromo.setVisibility(View.GONE);
                        }

                        tvOperatingHours.setText(hours != null && !hours.isEmpty() ? hours : "11:00 AM to 9:00 PM");

                        Glide.with(this).load(coverUrl != null && !coverUrl.isEmpty() ? coverUrl : DEFAULT_COVER).centerCrop().into(ivCoverPhoto);
                        Glide.with(this).load(logoUrl != null && !logoUrl.isEmpty() ? logoUrl : DEFAULT_LOGO).centerCrop().into(ivVenueLogo);
                    }
                });
    }

    private void loadTrendingItems() {
        FirebaseFirestore.getInstance().collection("menu").limit(4).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<MenuItem> trendingList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        MenuItem item = new MenuItem();
                        item.setName(doc.getString("name"));
                        Object priceObj = doc.get("price");
                        item.setPrice(priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0.0);
                        item.setImageUrl(doc.getString("imageUrl"));
                        trendingList.add(item);
                    }
                    recyclerTrending.setAdapter(new TrendingAdapter(trendingList));
                });
    }

    private class TrendingAdapter extends RecyclerView.Adapter<TrendingAdapter.ViewHolder> {
        private List<MenuItem> items;
        TrendingAdapter(List<MenuItem> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MenuItem item = items.get(position);
            holder.tvName.setText(item.getName() != null ? item.getName() : "Special");
            holder.tvPrice.setText(String.format(Locale.US, "৳%.2f", item.getPrice()));

            String img = item.getImageUrl();
            if (img != null && !img.isEmpty()) {
                Glide.with(holder.itemView.getContext()).load(img).centerCrop().into(holder.ivImage);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage; TextView tvName, tvPrice;
            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivTrendingImage);
                tvName = itemView.findViewById(R.id.tvTrendingName);
                tvPrice = itemView.findViewById(R.id.tvTrendingPrice);
            }
        }
    }
}