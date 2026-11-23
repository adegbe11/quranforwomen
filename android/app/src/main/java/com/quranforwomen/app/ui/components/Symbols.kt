package com.quranforwomen.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.BorderColor
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The iPhone app and the bundled content name icons by SF Symbol. This maps each one to the
 * closest Material icon, so content.json and journal data work unchanged on Android.
 */
fun symbol(name: String): ImageVector = when (name) {
    "arrow.counterclockwise" -> Icons.Outlined.Replay
    "arrow.down.circle", "arrow.down.to.line" -> Icons.Outlined.Download
    "arrow.right", "arrow.right.circle" -> Icons.AutoMirrored.Outlined.ArrowForward
    "arrow.right.circle.fill" -> Icons.Filled.ArrowCircleRight
    "arrow.triangle.2.circlepath" -> Icons.Outlined.Sync
    "arrow.uturn.right" -> Icons.AutoMirrored.Outlined.Redo
    "arrowtriangle.down.fill" -> Icons.Filled.ArrowDropDown
    "backward.fill" -> Icons.Filled.SkipPrevious
    "forward.fill" -> Icons.Filled.SkipNext
    "bell.badge" -> Icons.Outlined.NotificationsActive
    "book" -> Icons.AutoMirrored.Outlined.MenuBook
    "book.closed" -> Icons.Outlined.Book
    "book.pages" -> Icons.Outlined.AutoStories
    "bookmark" -> Icons.Outlined.BookmarkBorder
    "bookmark.fill" -> Icons.Filled.Bookmark
    "bookmark.slash" -> Icons.Outlined.BookmarkRemove
    "building.columns" -> Icons.Outlined.AccountBalance
    "calendar" -> Icons.Outlined.CalendarToday
    "checkmark" -> Icons.Rounded.Check
    "checkmark.circle.fill" -> Icons.Filled.CheckCircle
    "chevron.up.chevron.down" -> Icons.Outlined.UnfoldMore
    "circle.dotted" -> Icons.Outlined.Circle
    "clock" -> Icons.Outlined.Schedule
    "clock.arrow.circlepath" -> Icons.Outlined.History
    "cloud.rain" -> Icons.Outlined.Cloud
    "doc" -> Icons.Outlined.Description
    "doc.on.doc" -> Icons.Outlined.ContentCopy
    "doc.text" -> Icons.AutoMirrored.Outlined.Article
    "drop" -> Icons.Outlined.WaterDrop
    "ellipsis.circle" -> Icons.Outlined.MoreHoriz
    "figure.and.child.holdinghands" -> Icons.Outlined.FamilyRestroom
    "flame" -> Icons.Outlined.LocalFireDepartment
    "gearshape" -> Icons.Outlined.Settings
    "globe" -> Icons.Outlined.Language
    "graduationcap" -> Icons.Outlined.AutoStories
    "hand.tap" -> Icons.Outlined.TouchApp
    "hands.and.sparkles" -> Icons.Outlined.VolunteerActivism
    "headphones" -> Icons.Outlined.Headphones
    "heart" -> Icons.Outlined.FavoriteBorder
    "highlighter" -> Icons.Outlined.BorderColor
    "house" -> Icons.Outlined.Home
    "leaf" -> Icons.Outlined.Eco
    "lightbulb" -> Icons.Outlined.Lightbulb
    "line.3.horizontal.decrease.circle" -> Icons.Outlined.FilterList
    "location" -> Icons.Outlined.NearMe
    "location.north.line" -> Icons.Outlined.Explore
    "lock" -> Icons.Outlined.Lock
    "magnifyingglass" -> Icons.Outlined.Search
    "mic" -> Icons.Outlined.Mic
    "mic.fill" -> Icons.Filled.Mic
    "moon" -> Icons.Outlined.DarkMode
    "moon.fill" -> Icons.Filled.DarkMode
    "moon.stars" -> Icons.Outlined.NightsStay
    "moon.zzz" -> Icons.Outlined.Bedtime
    "note.text" -> Icons.AutoMirrored.Outlined.Notes
    "pencil" -> Icons.Outlined.Edit
    "pencil.and.scribble" -> Icons.Outlined.EditNote
    "person" -> Icons.Outlined.Person
    "person.3" -> Icons.Outlined.Groups
    "person.crop.circle" -> Icons.Outlined.AccountCircle
    "person.wave.2" -> Icons.Outlined.RecordVoiceOver
    "photo" -> Icons.Outlined.Image
    "pin.fill" -> Icons.Filled.PushPin
    "play.fill" -> Icons.Filled.PlayArrow
    "pause.fill" -> Icons.Filled.Pause
    "stop.fill" -> Icons.Filled.Stop
    "plus" -> Icons.Rounded.Add
    "questionmark" -> Icons.Rounded.QuestionMark
    "questionmark.circle" -> Icons.AutoMirrored.Outlined.HelpOutline
    "repeat" -> Icons.Outlined.Repeat
    "shield" -> Icons.Outlined.Shield
    "signpost.right" -> Icons.Outlined.Explore
    "slider.horizontal.3" -> Icons.Outlined.Tune
    "sparkles" -> Icons.Outlined.AutoAwesome
    "square.and.arrow.up" -> Icons.Outlined.Share
    "square.and.pencil" -> Icons.Outlined.EditNote
    "star.fill" -> Icons.Filled.Star
    "sun.horizon", "sunrise" -> Icons.Outlined.WbTwilight
    "sun.max" -> Icons.Outlined.WbSunny
    "sun.min" -> Icons.Outlined.LightMode
    "target" -> Icons.Outlined.TrackChanges
    "text.alignleft" -> Icons.AutoMirrored.Outlined.Notes
    "text.bubble" -> Icons.Outlined.ChatBubbleOutline
    "text.quote" -> Icons.Outlined.FormatQuote
    "textformat.size", "textformat.size.larger", "textformat.size.smaller" -> Icons.Outlined.FormatSize
    "textformat" -> Icons.Outlined.TextFields
    "trash" -> Icons.Outlined.Delete
    "water.waves" -> Icons.Outlined.Waves
    "waveform" -> Icons.Outlined.GraphicEq
    "wifi.exclamationmark" -> Icons.Outlined.WifiOff
    "wind" -> Icons.Outlined.Air
    "xmark" -> Icons.Rounded.Close
    "xmark.circle", "xmark.circle.fill" -> Icons.Filled.Cancel
    "spa" -> Icons.Outlined.Spa
    else -> Icons.Outlined.Circle
}
