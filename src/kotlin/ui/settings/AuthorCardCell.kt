package desu.inugram.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.AvatarDrawable
import org.telegram.ui.Components.BackupImageView
import org.telegram.ui.Components.LayoutHelper
import java.util.Locale

@SuppressLint("ViewConstructor")
class AuthorCardCell(
    context: Context,
    private val resourcesProvider: Theme.ResourcesProvider?,
    private val username: String,
    onOpen: () -> Unit,
) : FrameLayout(context), NotificationCenter.NotificationCenterDelegate {

    private val brand = 0xFFFA456C.toInt()

    private var account = -1
    private var userId = 0L
    private var requested = false

    private val avatarView = BackupImageView(context).apply {
        setRoundRadius(AndroidUtilities.dp(30f))
    }

    private val ringView = FrameLayout(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0)
            setStroke(AndroidUtilities.dp(2f), brand)
        }
        addView(avatarView, LayoutHelper.createFrame(60, 60f, Gravity.CENTER))
    }

    private val chipView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
        setTypeface(AndroidUtilities.bold())
        setTextColor(brand)
        letterSpacing = 0.12f
        maxLines = 1
        text = LocaleController.getString(R.string.InuAboutAuthor).uppercase(Locale.getDefault())
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = AndroidUtilities.dp(8f).toFloat()
            setColor(ColorUtils.setAlphaComponent(brand, 0x24))
        }
        setPadding(
            AndroidUtilities.dp(8f),
            AndroidUtilities.dp(2f),
            AndroidUtilities.dp(8f),
            AndroidUtilities.dp(2f),
        )
    }

    private val nameView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)
        setTypeface(AndroidUtilities.bold())
        setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider))
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        text = username
    }

    private val handleView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        setTextColor(brand)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        text = "@$username"
    }

    private val actionView = FrameLayout(context).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ColorUtils.setAlphaComponent(brand, 0x24))
        }
        addView(
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setImageResource(R.drawable.inu_tabler_brand_telegram)
                setColorFilter(brand)
            },
            LayoutHelper.createFrame(20, 20f, Gravity.CENTER),
        )
    }

    private val card = FrameLayout(context).apply {
        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                ColorUtils.setAlphaComponent(brand, 0x2E),
                ColorUtils.setAlphaComponent(brand, 0x0D),
            ),
        ).apply {
            cornerRadius = AndroidUtilities.dp(20f).toFloat()
            setStroke(AndroidUtilities.dp(1f), ColorUtils.setAlphaComponent(brand, 0x40))
        }
        foreground = Theme.createRadSelectorDrawable(
            Theme.getColor(Theme.key_listSelector, resourcesProvider),
            20,
            20,
        )
        isClickable = true
        isFocusable = true
        contentDescription = "${LocaleController.getString(R.string.InuAboutAuthor)} @$username"
        setOnClickListener { onOpen() }

        addView(
            ringView,
            LayoutHelper.createFrame(68, 68f, Gravity.START or Gravity.CENTER_VERTICAL, 14f, 0f, 0f, 0f),
        )

        val texts = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(chipView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT))
            addView(
                nameView,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 6f, 0f, 0f),
            )
            addView(
                handleView,
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0f, 1f, 0f, 0f),
            )
        }
        addView(
            texts,
            LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT.toFloat(),
                Gravity.START or Gravity.CENTER_VERTICAL,
                96f, 0f, 62f, 0f,
            ),
        )
        addView(
            actionView,
            LayoutHelper.createFrame(36, 36f, Gravity.END or Gravity.CENTER_VERTICAL, 0f, 0f, 16f, 0f),
        )
    }

    init {
        addView(card, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 88f, Gravity.CENTER, 16f, 0f, 16f, 0f))
        showPlaceholder()
    }

    private fun showPlaceholder() {
        avatarView.setImageDrawable(
            AvatarDrawable().apply {
                setInfo(0L, username, null)
                setColor(brand)
            }
        )
    }

    private fun bind() {
        if (account < 0 || userId == 0L) return
        val user = MessagesController.getInstance(account).getUser(userId) ?: return
        avatarView.imageReceiver.currentAccount = account
        avatarView.setForUserOrChat(user, AvatarDrawable().apply { setInfo(user) })
        val name = UserObject.getUserName(user)
        if (name.isNotBlank()) nameView.text = name
    }

    private fun load() {
        if (requested) return
        val acc = UserConfig.selectedAccount
        if (!UserConfig.getInstance(acc).isClientActivated) return
        requested = true
        account = acc
        MessagesController.getInstance(acc).userNameResolver.resolve(username, null, true) { peerId ->
            if (peerId != null && peerId > 0L && peerId != Long.MAX_VALUE) {
                userId = peerId
                bind()
            } else {
                requested = false
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(104f), MeasureSpec.EXACTLY),
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        for (i in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            NotificationCenter.getInstance(i).addObserver(this, NotificationCenter.updateInterfaces)
        }
        load()
        bind()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        for (i in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            NotificationCenter.getInstance(i).removeObserver(this, NotificationCenter.updateInterfaces)
        }
    }

    override fun didReceivedNotification(id: Int, account: Int, vararg args: Any) {
        if (id != NotificationCenter.updateInterfaces || account != this.account) return
        val mask = args[0] as? Int ?: return
        if (mask and (MessagesController.UPDATE_MASK_AVATAR or MessagesController.UPDATE_MASK_NAME) != 0) {
            bind()
        }
    }
}
