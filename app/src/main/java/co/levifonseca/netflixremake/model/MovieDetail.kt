package co.levifonseca.netflixremake.model

data class MovieDetail(
    val movie: Movie,
    val similars: List<Movie>
)