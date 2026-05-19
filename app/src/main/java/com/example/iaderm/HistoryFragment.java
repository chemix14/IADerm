package com.example.iaderm;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iaderm.data.AnalysisRecord;
import com.example.iaderm.viewmodel.HistoryViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {
    private static final SimpleDateFormat ITEM_DATE_FORMAT =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("es", "MX"));

    private final HistoryRecordAdapter adapter = new HistoryRecordAdapter();
    private HistoryViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        ImageButton btnExportPdf = view.findViewById(R.id.btnExportPdf);
        RecyclerView recyclerHistory = view.findViewById(R.id.recyclerHistory);
        TrendChartView trendChart = view.findViewById(R.id.trendChart);
        TextView tvAvgScore = view.findViewById(R.id.tvAvgScore);
        TextView tvTotalAnalyses = view.findViewById(R.id.tvTotalAnalyses);
        TextView tvTrend = view.findViewById(R.id.tvTrend);
        View emptyState = view.findViewById(R.id.emptyState);
        ChipGroup cgTimeFilter = view.findViewById(R.id.cgTimeFilter);

        btnExportPdf.setOnClickListener(v -> {
            UiFeedback.shortMessage(getContext(), R.string.capture_processing);
            viewModel.generatePdf();
        });

        cgTimeFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipFilter7Days) {
                viewModel.setFilterDays(7);
            } else if (checkedId == R.id.chipFilter30Days) {
                viewModel.setFilterDays(30);
            } else {
                viewModel.setFilterDays(0);
            }
        });

        recyclerHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerHistory.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);
        viewModel.getAllAnalyses().observe(getViewLifecycleOwner(), records -> {
            boolean hasData = records != null && !records.isEmpty();
            emptyState.setVisibility(hasData ? View.GONE : View.VISIBLE);
            recyclerHistory.setVisibility(hasData ? View.VISIBLE : View.GONE);

            if (!hasData) {
                trendChart.setScores(new int[]{0});
                tvAvgScore.setText("0");
                tvTotalAnalyses.setText("0");
                tvTrend.setText(R.string.history_trend_flat);
                adapter.submitList(java.util.Collections.emptyList());
                return;
            }

            adapter.submitList(records);
            trendChart.setScores(toScoreArray(records));
            tvTotalAnalyses.setText(String.valueOf(records.size()));
            tvAvgScore.setText(String.valueOf(calculateAverage(records)));
            tvTrend.setText(calculateTrendText(records));
        });

        viewModel.getPdfGeneratedPath().observe(getViewLifecycleOwner(), path -> {
            if (path != null) {
                sharePdf(path);
                viewModel.resetPdfPath();
            }
        });

        return view;
    }

    private void sharePdf(String path) {
        java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            Toast.makeText(getContext(), "Error: PDF file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(intent, "Compartir Reporte"));
    }

    private int[] toScoreArray(List<AnalysisRecord> records) {
        int[] scores = new int[records.size()];
        for (int i = 0; i < records.size(); i++) {
            scores[i] = records.get(records.size() - 1 - i).score;
        }
        return scores;
    }

    private int calculateAverage(List<AnalysisRecord> records) {
        if (records.isEmpty()) return 0;
        int sum = 0;
        for (AnalysisRecord record : records) sum += record.score;
        return Math.round((float) sum / records.size());
    }

    private String calculateTrendText(List<AnalysisRecord> records) {
        if (records.size() < 2) return getString(R.string.history_trend_flat);
        int latest = records.get(0).score;
        int oldest = records.get(records.size() - 1).score;
        if (oldest == 0) return getString(R.string.history_trend_flat);
        int percentage = Math.round(((latest - oldest) / (float) oldest) * 100f);
        if (percentage == 0) return getString(R.string.history_trend_flat);
        return getString(percentage > 0 ? R.string.history_trend_up : R.string.history_trend_down,
                Math.abs(percentage));
    }

    private static class HistoryRecordAdapter extends RecyclerView.Adapter<HistoryRecordAdapter.Holder> {
        private List<AnalysisRecord> records = java.util.Collections.emptyList();

        void submitList(List<AnalysisRecord> items) {
            records = items;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_record, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            AnalysisRecord record = records.get(position);
            int score = Math.max(0, Math.min(100, record.score));
            holder.tvItemScore.setText(String.valueOf(score));
            holder.tvItemDate.setText(ITEM_DATE_FORMAT.format(new Date(record.timestamp)));

            if (score <= 35) {
                holder.chipItemSeverity.setText(R.string.severity_mild);
                holder.chipItemSeverity.setChipBackgroundColorResource(R.color.severity_mild_bg);
                holder.chipItemSeverity.setTextColor(holder.itemView.getContext().getColor(R.color.severity_mild_text));
            } else if (score <= 65) {
                holder.chipItemSeverity.setText(R.string.severity_moderate);
                holder.chipItemSeverity.setChipBackgroundColorResource(R.color.severity_moderate_bg);
                holder.chipItemSeverity.setTextColor(holder.itemView.getContext().getColor(R.color.severity_moderate_text));
            } else {
                holder.chipItemSeverity.setText(R.string.severity_severe);
                holder.chipItemSeverity.setChipBackgroundColorResource(R.color.severity_severe_bg);
                holder.chipItemSeverity.setTextColor(holder.itemView.getContext().getColor(R.color.severity_severe_text));
            }

            holder.btnItemCompare.setOnClickListener(v ->
                    UiFeedback.shortMessage(v.getContext(), R.string.history_compare));

            holder.itemView.setOnClickListener(v ->
                    AppNavigator.openResults(v.getContext(), record.id, score, record.diagnosis, record.heatmapData));
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            TextView tvItemScore;
            TextView tvItemDate;
            Chip chipItemSeverity;
            MaterialButton btnItemCompare;

            Holder(View itemView) {
                super(itemView);
                tvItemScore = itemView.findViewById(R.id.tvItemScore);
                tvItemDate = itemView.findViewById(R.id.tvItemDate);
                chipItemSeverity = itemView.findViewById(R.id.chipItemSeverity);
                btnItemCompare = itemView.findViewById(R.id.btnItemCompare);
            }
        }
    }
}
