package app.philm.in.adapters;

import com.hb.views.PinnedSectionListView;
import com.jakewharton.trakt.entities.Movie;
import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import app.philm.in.Constants;
import app.philm.in.R;
import app.philm.in.controllers.MovieController;
import app.philm.in.trakt.TraktImageHelper;
import app.philm.in.util.PhilmCollections;

public class MovieSectionedListAdapter extends BaseAdapter implements
        PinnedSectionListView.PinnedSectionListAdapter {

    private static final String LOG_TAG = MovieSectionedListAdapter.class.getSimpleName();

    private static final class Item {

        static final int TYPE_ITEM = 0;

        static final int TYPE_SECTION = 1;

        private final int type;

        private final Movie movie;

        private final int titleResId;

        Item(Movie movie) {
            type = TYPE_ITEM;
            this.movie = movie;
            titleResId = 0;
        }

        Item(int sectionTitle) {
            type = TYPE_SECTION;
            titleResId = sectionTitle;
            movie = null;
        }
    }

    private final Activity mActivity;
    private final TraktImageHelper mTraktImageHelper;
    private final DateFormat mDateFormat;

    private List<Item> mItems;

    public MovieSectionedListAdapter(Activity activity) {
        mActivity = activity;
        mTraktImageHelper = new TraktImageHelper(activity.getResources());
        mDateFormat = android.text.format.DateFormat.getMediumDateFormat(activity);
    }

    public void setItems(List<Movie> items) {
        setItems(items, null);
    }

    public void setItems(List<Movie> items, List<MovieController.Filter> sections) {
        if (PhilmCollections.isEmpty(items)) {
            mItems = null;
        } else {
            mItems = new ArrayList<Item>();

            if (!PhilmCollections.isEmpty(sections)) {
                HashSet<Movie> movies = new HashSet<Movie>(items);
                for (MovieController.Filter filter : sections) {
                    boolean addedHeader = false;
                    for (Iterator<Movie> i = movies.iterator(); i.hasNext(); ) {
                        Movie movie = i.next();
                        if (filter.isMovieFiltered(movie)) {
                            if (!addedHeader) {
                                mItems.add(new Item(filter.getTitle()));
                                addedHeader = true;
                            }
                            mItems.add(new Item(movie));
                            i.remove();
                        }
                    }
                }
            } else {
                for (Movie movie : items) {
                    mItems.add(new Item(movie));
                }
            }
        }
        notifyDataSetChanged();
    }

    private static List<Movie> upcomingFilms(List<Movie> movies) {
        ArrayList<Movie> upcoming = null;
        for (Movie movie : movies) {
            final long time = movie.released.getTime();
            if (time - Constants.FUTURE_SOON_THRESHOLD > System.currentTimeMillis()) {
                if (upcoming == null) {
                    upcoming = new ArrayList<Movie>();
                }
                upcoming.add(movie);
            }
        }
        return upcoming;
    }

    private static List<Movie> soonFilms(List<Movie> movies) {
        ArrayList<Movie> soon = null;
        for (Movie movie : movies) {
            final long time = movie.released.getTime();
            if (time > System.currentTimeMillis()
                    && (time - Constants.FUTURE_SOON_THRESHOLD <= System.currentTimeMillis())) {
                if (soon == null) {
                    soon = new ArrayList<Movie>();
                }
                soon.add(movie);
            }
        }
        return soon;
    }

    private static List<Movie> releasedFilms(List<Movie> movies) {
        ArrayList<Movie> released = null;
        for (Movie movie : movies) {
            if (movie.released.getTime() <= System.currentTimeMillis()) {
                if (released == null) {
                    released = new ArrayList<Movie>();
                }
                released.add(movie);
            }
        }
        return released;
    }

    @Override
    public int getCount() {
        return mItems != null ? mItems.size() : 0;
    }

    @Override
    public Item getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        final Item item = getItem(position);
        View view = convertView;

        if (view == null) {
            final int layout = item.type == Item.TYPE_ITEM
                    ? R.layout.item_list_movie
                    : R.layout.item_list_movie_section_header;
            view = mActivity.getLayoutInflater().inflate(layout, viewGroup, false);
        }

        switch (item.type) {
            case Item.TYPE_ITEM: {
                Movie movie = item.movie;

                final TextView title = (TextView) view.findViewById(R.id.textview_title);
                title.setText(mActivity.getString(R.string.movie_title_year, movie.title,
                        movie.year));

                final TextView rating = (TextView) view.findViewById(R.id.textview_rating);
                rating.setText(mActivity.getString(R.string.movie_rating_votes,
                        movie.ratings.percentage,
                        movie.ratings.votes));

                final TextView release = (TextView) view.findViewById(R.id.textview_release);
                release.setText(mActivity.getString(R.string.movie_release_date,
                        mDateFormat.format(movie.released)));

                final ImageView imageView = (ImageView) view.findViewById(R.id.imageview_poster);
                Picasso.with(mActivity)
                        .load(mTraktImageHelper.getPosterUrl(item.movie))
                        .into(imageView);

                break;
            }
            case Item.TYPE_SECTION:
                ((TextView) view).setText(item.titleResId);
                break;
        }

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type;
    }

    @Override
    public boolean isItemViewTypePinned(int type) {
        return type == Item.TYPE_SECTION;
    }
}
