package ja.spaarkssticker.stories

import java.io.IOException

sealed interface SaveFileResult {

    object Success : SaveFileResult
    class Failure(val exception: IOException) : SaveFileResult

}