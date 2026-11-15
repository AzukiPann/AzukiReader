package io.legado.app.feature.readaloud.wenku8

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.legado.app.R
import io.legado.app.core.designsystem.wenku8.chrome.Wenku8ArchiveSection
import io.legado.app.domain.gateway.*
import io.legado.app.ui.widget.components.modalBottomSheet.AppModalBottomSheet
import io.legado.app.ui.widget.components.player.AnimatedPlayPauseButton
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

@Composable
fun Wenku8ListeningRoute(reader:Boolean, onDismiss:()->Unit, onOfflineSettings:()->Unit, modifier:Modifier=Modifier, showHeader:Boolean=true) {
    val vm:Wenku8ListeningViewModel=koinViewModel(key="wenku8-listening-$reader",parameters={parametersOf(reader)})
    val state by vm.uiState.collectAsStateWithLifecycle()
    Wenku8ListeningPanel(state,vm::onIntent,onDismiss,onOfflineSettings,modifier,reader,showHeader)
    DisposableEffect(Unit) { onDispose { vm.onIntent(ListeningIntent.StopPreview) } }
}

@Composable
fun Wenku8ListeningPanel(state:ListeningUiState,onIntent:(ListeningIntent)->Unit,onDismiss:()->Unit,
    onOfflineSettings:()->Unit,modifier:Modifier=Modifier,reader:Boolean=true,showHeader:Boolean=true) {
    val p=state.playback
    var voiceMenu by remember { mutableStateOf(false) }
    var waiting by remember { mutableStateOf(false) }
    LaunchedEffect(p.preparing) {
        waiting=false
        if(p.preparing) { kotlinx.coroutines.delay(250); waiting=true }
    }
    Surface(modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.surface) {
        LazyColumn(Modifier.fillMaxWidth().testTag("listening-panel"),
            contentPadding=PaddingValues(horizontal=20.dp,vertical=12.dp),
            verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(showHeader) item("heading") {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Wenku8ArchiveSection(stringResource(R.string.aloud_config),"READER / AUDIO",Modifier.weight(1f))
                    IconButton(onClick=onDismiss) { Icon(Icons.Default.Close,stringResource(R.string.w8_listening_close)) }
                }
            }
            item("playback") {
                ListeningCard {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(Icons.Default.Headphones,null,Modifier.padding(12.dp).size(24.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(p.bookName,fontSize=18.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                            Text(if(p.active) stringResource(R.string.w8_listening_chapter,p.chapterIndex+1,p.chapterTitle)
                                else stringResource(R.string.w8_listening_start_chapter,p.chapterIndex+1),
                                fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                    }
                    Text(stringResource(when { waiting -> R.string.w8_listening_preparing; !p.active -> R.string.w8_listening_ready; p.paused -> R.string.w8_listening_paused; else -> R.string.w8_listening_playing }),
                        fontSize=11.sp,fontWeight=FontWeight.Medium,color=MaterialTheme.colorScheme.primary)
                    Text(p.currentText.ifBlank { stringResource(R.string.w8_listening_help) },
                        maxLines=2,minLines=2,overflow=TextOverflow.Ellipsis,fontSize=13.sp,lineHeight=21.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                        TransportIcon(Icons.Default.SkipPrevious,R.string.w8_listening_previous,p.active&&!p.preparing) {onIntent(ListeningIntent.Move(-1))}
                        AnimatedPlayPauseButton(p.active&&!p.paused,stringResource(if(p.active&&!p.paused)R.string.w8_listening_pause else R.string.w8_listening_play),
                            {onIntent(ListeningIntent.PlayPause)},Modifier.size(56.dp))
                        TransportIcon(Icons.Default.SkipNext,R.string.w8_listening_next,p.active&&!p.preparing) {onIntent(ListeningIntent.Move(1))}
                        TransportIcon(Icons.Default.Stop,R.string.w8_listening_stop,p.active) {onIntent(ListeningIntent.Stop)}
                    }
                    if(p.active) {
                        ListeningSlider(stringResource(R.string.w8_listening_progress),p.fraction.coerceIn(0f,1f),0f..1f,
                            { String.format(Locale.ROOT,"%.1f%%",it*100) }) {onIntent(ListeningIntent.Seek(it))}
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                            Text(stringResource(R.string.w8_listening_segment,p.segmentIndex+1,p.segmentCount),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick={onIntent(ListeningIntent.Skip)}) { Text(stringResource(R.string.w8_listening_skip)) }
                        }
                        Text(if(p.preparing)stringResource(R.string.w8_listening_clip_preparing) else stringResource(R.string.w8_listening_clip,time(p.clipPositionMs),time(p.clipDurationMs)),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if(reader) {
                        HorizontalDivider()
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick={onIntent(ListeningIntent.FromReader(false))}) {Text(stringResource(R.string.w8_listening_from_here))}
                            p.resumeChapter?.let { chapter -> TextButton(onClick={onIntent(ListeningIntent.FromReader(true))}) {Text(stringResource(R.string.w8_listening_resume,chapter+1))} }
                        }
                        if(p.otherBookPlaying) Text(stringResource(R.string.w8_listening_other_book),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    (p.error ?: state.message)?.let { Text(it,color=MaterialTheme.colorScheme.error,fontSize=13.sp) }
                    if(p.skippedSegments>0) Text(stringResource(R.string.w8_listening_skipped,p.skippedSegments),fontSize=12.sp)
                }
            }
            item("voice") {
                ListeningCard {
                    Text(stringResource(R.string.w8_listening_voices),fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                    Text(stringResource(R.string.aloud_unified_hint), fontSize=12.sp,
                        lineHeight=19.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
                    if(p.voices.isNotEmpty()) Box {
                        OutlinedButton(onClick={voiceMenu=true},modifier=Modifier.fillMaxWidth().testTag("listening-voice-picker")) {
                            Text(p.voiceLabel,Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                            Icon(Icons.Default.ExpandMore,null)
                        }
                        DropdownMenu(voiceMenu,{voiceMenu=false},modifier=Modifier.heightIn(max=280.dp)) {
                            p.voices.forEach { voice -> DropdownMenuItem(
                                text={Text("${voice.name} · ${voice.source}")},
                                leadingIcon={RadioButton(voice.selected,null)},
                                modifier=Modifier.semantics { selected=voice.selected; role=Role.RadioButton },
                                onClick={voiceMenu=false;onIntent(ListeningIntent.Voice(voice.id))}) }
                        }
                    }
                    OutlinedButton(onClick=onOfflineSettings, modifier=Modifier.fillMaxWidth()
                        .testTag("listening-manage-voices")) {
                        Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.aloud_unified_manage))
                    }

                }
            }
            item("preferences") {
                ListeningCard {
                    ListeningSlider(stringResource(R.string.w8_listening_speed),p.speed.coerceIn(.5f,3f),.5f..3f,
                        { String.format(Locale.ROOT,"%.2f×",it) }) {onIntent(ListeningIntent.Speed(it))}
                    HorizontalDivider()
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.w8_listening_follow),fontSize=14.sp,fontWeight=FontWeight.Medium)
                            Text(stringResource(R.string.w8_listening_follow_hint),fontSize=12.sp,lineHeight=18.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val followLabel=stringResource(R.string.w8_listening_follow)
                        Switch(p.follow,{onIntent(ListeningIntent.Follow(it))},modifier=Modifier.semantics {contentDescription=followLabel})
                    }
                }
            }
            item("sleep") {
                ListeningCard {
                    Text(stringResource(R.string.w8_listening_sleep),fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                    Text(when {p.sleepAfterChapter->stringResource(R.string.w8_listening_sleep_chapter);p.timerMinutes>0->stringResource(R.string.w8_listening_sleep_minutes,p.timerMinutes);else->stringResource(R.string.w8_listening_no_timer)},fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(p.timerMinutes==0&&!p.sleepAfterChapter,{onIntent(ListeningIntent.Sleep())},{Text(stringResource(R.string.w8_listening_timer_off))})
                        for(minutes in listOf(15,30,60)) FilterChip(p.timerMinutes==minutes&&!p.sleepAfterChapter,{onIntent(ListeningIntent.Sleep(minutes))},{Text(stringResource(R.string.w8_listening_minutes,minutes))})
                        FilterChip(p.sleepAfterChapter,{onIntent(ListeningIntent.Sleep(afterChapter=true))},{Text(stringResource(R.string.w8_listening_chapter_end))})
                    }
                }
            }
        }
    }
}

@Composable private fun ListeningCard(content:@Composable ColumnScope.()->Unit) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),
        border=BorderStroke(1.dp,MaterialTheme.colorScheme.onSurface.copy(alpha=.08f)),
        color=MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)
    }
}

@Composable private fun sourceName(source:String)=stringResource(when(source){"edge"->R.string.w8_listening_online;"custom"->R.string.w8_listening_custom;"system"->R.string.w8_listening_offline;else->R.string.w8_listening_existing})
@Composable private fun TransportIcon(icon:ImageVector,label:Int,enabled:Boolean,onClick:()->Unit) {
    IconButton(onClick,enabled=enabled) {Icon(icon,stringResource(label))}
}
@Composable private fun ListeningSlider(label:String,value:Float,range:ClosedFloatingPointRange<Float>,format:(Float)->String,steps:Int=0,onCommit:(Float)->Unit) {
    var draft by remember {mutableFloatStateOf(value)}
    var dragging by remember {mutableStateOf(false)}
    LaunchedEffect(value, dragging) { if (!dragging) draft=value }
    Column {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {Text(label,fontSize=13.sp);Text(format(draft),fontFamily=FontFamily.Monospace,fontSize=12.sp)}
        Slider(draft,{dragging=true;draft=it},valueRange=range,steps=steps,onValueChangeFinished={onCommit(draft);dragging=false},modifier=Modifier.semantics {contentDescription=label})
    }
}
private fun time(ms:Long)="${ms/60000}:${((ms/1000)%60).toString().padStart(2,'0')}"

@Composable private fun ListeningCustomSheet(state:ListeningUiState,onIntent:(ListeningIntent)->Unit) {
    val existing=state.playback.custom
    var endpoint by rememberSaveable {mutableStateOf(existing?.endpoint.orEmpty())}
    var voice by rememberSaveable {mutableStateOf(existing?.voiceId ?: "zh-CN-XiaoxiaoNeural")}
    var locale by rememberSaveable {mutableStateOf(existing?.locale ?: "zh-CN")}
    var label by rememberSaveable {mutableStateOf(existing?.label ?: "自定义音色")}
    var auth by rememberSaveable {mutableStateOf(existing?.authentication ?: "none")}
    // Credentials are deliberately absent from saved state and the observable UI snapshot.
    var secret by remember {mutableStateOf("")}
    var sample by rememberSaveable {mutableStateOf("你好，这是语音接口试听。")}
    fun config()=ListeningCustomConfig(endpoint.trim(),voice.trim(),locale.trim(),label.trim(),auth,existing?.hasSecret==true)
    AppModalBottomSheet(show=true,onDismissRequest={onIntent(ListeningIntent.CloseCustom)},title=stringResource(R.string.w8_listening_custom_config)) {
        LazyColumn(Modifier.heightIn(max=560.dp).fillMaxWidth(),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {Text(stringResource(R.string.w8_listening_custom_intro),fontSize=12.sp)}
            item {OutlinedTextField(endpoint,{endpoint=it},label={Text(stringResource(R.string.w8_listening_endpoint))},singleLine=true,modifier=Modifier.fillMaxWidth())}
            item {OutlinedTextField(voice,{voice=it},label={Text(stringResource(R.string.w8_listening_voice_id))},singleLine=true,modifier=Modifier.fillMaxWidth())}
            item {OutlinedTextField(locale,{locale=it},label={Text(stringResource(R.string.w8_listening_locale))},singleLine=true,modifier=Modifier.fillMaxWidth())}
            item {OutlinedTextField(label,{label=it},label={Text(stringResource(R.string.w8_listening_voice_label))},singleLine=true,modifier=Modifier.fillMaxWidth())}
            item {FlowRow {for((id,title) in listOf("none" to R.string.w8_listening_auth_none,"subscriptionKey" to R.string.w8_listening_auth_key,"bearer" to R.string.w8_listening_auth_bearer)) FilterChip(auth==id,{auth=id},{Text(stringResource(title))})}}
            if(auth!="none") item {OutlinedTextField(secret,{secret=it},label={Text(stringResource(if(existing?.hasSecret==true)R.string.w8_listening_keep_secret else R.string.w8_listening_secret))},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())}
            item {OutlinedTextField(sample,{sample=it.take(240)},label={Text(stringResource(R.string.w8_listening_preview_text))},modifier=Modifier.fillMaxWidth())}
            state.message?.let { item {Text(it,color=MaterialTheme.colorScheme.error)} }
            item {FlowRow {
                TextButton(enabled=!state.busy,onClick={onIntent(ListeningIntent.PreviewCustom(config(),secret,sample))}) {Text(stringResource(R.string.w8_listening_preview))}
                TextButton(onClick={onIntent(ListeningIntent.StopPreview)}) {Text(stringResource(R.string.w8_listening_preview_stop))}
                Button(enabled=!state.busy,onClick={onIntent(ListeningIntent.SaveCustom(config(),secret))}) {Text(stringResource(R.string.w8_listening_save))}
                if(existing!=null) TextButton(enabled=!state.busy,onClick={onIntent(ListeningIntent.DeleteCustom)}) {Text(stringResource(R.string.w8_listening_delete))}
            }}
        }
    }
}
