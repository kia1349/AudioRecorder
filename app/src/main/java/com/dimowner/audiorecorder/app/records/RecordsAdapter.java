/*
 * Copyright 2018 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimowner.audiorecorder.app.records;

import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dimowner.audiorecorder.R;
import com.dimowner.audiorecorder.app.widget.SimpleWaveformView;
import com.dimowner.audiorecorder.util.AndroidUtils;
import com.dimowner.audiorecorder.util.TimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecordsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private List<ListItem> data;

	private boolean showDateHeaders = true;
	private int activeItem = -1;

	private ItemClickListener itemClickListener;
	private OnAddToBookmarkListener onAddToBookmarkListener = null;

	public RecordsAdapter() {
		this.data = new ArrayList<>();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
		if (type == ListItem.ITEM_TYPE_HEADER) {

			View view = new View(viewGroup.getContext());
			int height;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				height = AndroidUtils.getStatusBarHeight(viewGroup.getContext()) + (int) viewGroup.getContext().getResources().getDimension(R.dimen.toolbar_height);
			} else {
				height = (int) viewGroup.getContext().getResources().getDimension(R.dimen.toolbar_height);
			}

			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, height);
			view.setLayoutParams(lp);
			return new UniversalViewHolder(view);
		} else if (type == ListItem.ITEM_TYPE_FOOTER) {
			View view = new View(viewGroup.getContext());
			int height = (int) viewGroup.getContext().getResources().getDimension(R.dimen.panel_height);

			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, height);
			view.setLayoutParams(lp);
			return new UniversalViewHolder(view);
		} else if (type == ListItem.ITEM_TYPE_DATE) {
			//Create date list item layout programmatically.
			TextView textView = new TextView(viewGroup.getContext());
			ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			textView.setLayoutParams(lp);
			textView.setTypeface(textView.getTypeface(), Typeface.BOLD);

			textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, viewGroup.getContext().getResources().getDimension(R.dimen.text_medium));

			int pad = (int) viewGroup.getContext().getResources().getDimension(R.dimen.spacing_small);
			textView.setPadding(pad, pad, pad, pad);
			textView.setGravity(Gravity.CENTER);

			return new UniversalViewHolder(textView);
		} else {
			View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false);
			return new ItemViewHolder(v);
		}
	}

	@Override
	public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int p) {
		if (viewHolder.getItemViewType() == ListItem.ITEM_TYPE_NORMAL) {
			final ItemViewHolder holder = (ItemViewHolder) viewHolder;
			holder.name.setText(data.get(p).getName());
			holder.description.setText(data.get(p).getDurationStr());
			holder.created.setText(data.get(p).getAddedTimeStr());
			if (data.get(p).isBookmarked()) {
				holder.bookmark.setImageResource(R.drawable.ic_bookmark_small);
			} else {
				holder.bookmark.setImageResource(R.drawable.ic_bookmark_bordered_small);
			}
			if (viewHolder.getLayoutPosition() == activeItem) {
				holder.view.setBackgroundResource(R.color.selected_item_color);
			} else {
				holder.view.setBackgroundResource(android.R.color.transparent);
			}

			holder.bookmark.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (onAddToBookmarkListener != null && data.size() > p) {
						if(data.get(p).isBookmarked()) {
							onAddToBookmarkListener.onRemoveFromBookmarks((int) data.get(p).getId());
						} else {
							onAddToBookmarkListener.onAddToBookmarks((int) data.get(p).getId());
						}
					}
				}
			});
			holder.waveformView.setWaveform(data.get(p).getAmps());

			holder.view.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					if (itemClickListener != null && data.size() > p) {
						int lpos = viewHolder.getLayoutPosition();
						itemClickListener.onItemClick(v, data.get(lpos).getId(), data.get(lpos).getPath(), lpos);
					}
				}});
		} else if (viewHolder.getItemViewType() == ListItem.ITEM_TYPE_DATE) {
			UniversalViewHolder holder = (UniversalViewHolder) viewHolder;
			((TextView)holder.view).setText(TimeUtils.formatDateSmart(data.get(p).getAdded(), holder.view.getContext()));
		}
	}

	public void setActiveItem(int activeItem) {
		int prev = this.activeItem;
		this.activeItem = activeItem;
		notifyItemChanged(prev);
		notifyItemChanged(activeItem);
	}

	public void setActiveItemById(long id) {
		int pos = findPositionById(id);
		if (pos >= 0) {
			setActiveItem(pos);
		}
	}

	public int findPositionById(long id) {
		if (id >= 0) {
			for (int i = 0; i < data.size() - 1; i++) {
				if (data.get(i).getId() == id) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	public int getItemCount() {
		return data.size();
	}

	@Override
	public int getItemViewType(int position) {
		return data.get(position).getType();
	}

	public void setData(List<ListItem> data) {
		this.data = data;
		if (showDateHeaders) {
			addDateHeaders();
		}
		this.data.add(0, ListItem.createHeaderItem());
		notifyDataSetChanged();
	}

	private void addDateHeaders() {
		if (data.size() > 0) {
			data.add(0, ListItem.createDateItem(data.get(0).getAdded()));
			Calendar d1 = Calendar.getInstance();
			d1.setTimeInMillis(data.get(0).getAdded());
			Calendar d2 = Calendar.getInstance();
			for (int i = 1; i < data.size(); i++) {
				d1.setTimeInMillis(data.get(i - 1).getAdded());
				d2.setTimeInMillis(data.get(i).getAdded());
				if (!TimeUtils.isSameDay(d1, d2)) {
					data.add(i, ListItem.createDateItem(data.get(i).getAdded()));
				}
			}
		}
	}

	public void deleteItem(long id) {
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).getId() == id) {
				data.remove(i);
				if (getAudioRecordsCount() == 0) {
					data.clear();
					notifyDataSetChanged();
				} else {
					notifyItemRemoved(i);
				}
			}
		}
	}

	public int getAudioRecordsCount() {
		int count = 0;
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).getType() == ListItem.ITEM_TYPE_NORMAL) {
				count++;
			}
		}
		return count;
	}

	public void showFooter() {
		if (findFooter() == -1) {
			this.data.add(ListItem.createFooterItem());
			notifyItemInserted(data.size()-1);
		}
	}

	public void hideFooter() {
		int pos = findFooter();
		if (pos != -1) {
			this.data.remove(pos);
			notifyItemRemoved(pos);
		}
	}

	public long getNextTo(long id) {
		if (id >= 0) {
			for (int i = 0; i < data.size() - 1; i++) {
				if (data.get(i).getId() == id) {
					if (data.get(i + 1).getId() == -1 && i+2 < data.size()) {
						return data.get(i + 2).getId();
					} else {
						return data.get(i + 1).getId();
					}
				}
			}
		}
		return -1;
	}

	public long getPrevTo(long id) {
		if (id >= 0) {
			for (int i = 1; i < data.size(); i++) {
				if (data.get(i).getId() == id) {
					if (data.get(i - 1).getId() == -1 && i-2 >= 0) {
						return data.get(i - 2).getId();
					} else {
						return data.get(i - 1).getId();
					}
				}
			}
		}
		return -1;
	}

	private int findFooter() {
		for (int i = data.size()-1; i>= 0; i--) {
			if (data.get(i).getType() == ListItem.ITEM_TYPE_FOOTER) {
				return i;
			}
		}
		return -1;
	}

	public void markAddedToBookmarks(int id) {
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).getId() == id) {
				data.get(i).setBookmarked(true);
				notifyItemChanged(i);
			}
		}
	}

	public void markRemovedFromBookmarks(int id) {
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).getId() == id) {
				data.get(i).setBookmarked(false);
				notifyItemChanged(i);
			}
		}
	}

	public void setItemClickListener(ItemClickListener itemClickListener) {
		this.itemClickListener = itemClickListener;
	}

	public void setOnAddToBookmarkListener(OnAddToBookmarkListener onAddToBookmarkListener) {
		this.onAddToBookmarkListener = onAddToBookmarkListener;
	}

	public interface ItemClickListener{
		void onItemClick(View view, long id, String path, int position);
	}


	public interface OnAddToBookmarkListener {
		void onAddToBookmarks(int id);
		void onRemoveFromBookmarks(int id);
	}

	public class ItemViewHolder extends RecyclerView.ViewHolder {
		TextView name;
		TextView description;
		TextView created;
		//		ImageView image;
		ImageButton bookmark;
		SimpleWaveformView waveformView;
		View view;

		public ItemViewHolder(View itemView) {
			super(itemView);
			this.view = itemView;
			this.name = itemView.findViewById(R.id.list_item_name);
			this.description = itemView.findViewById(R.id.list_item_description);
			this.created = itemView.findViewById(R.id.list_item_date);
//			this.image = itemView.findViewById(R.id.list_item_image);
			this.bookmark = itemView.findViewById(R.id.bookmark);
			this.waveformView = itemView.findViewById(R.id.list_item_waveform);
		}
	}

	public class UniversalViewHolder extends RecyclerView.ViewHolder {
		View view;

		public UniversalViewHolder(View view) {
			super(view);
			this.view = view;
		}
	}
}
