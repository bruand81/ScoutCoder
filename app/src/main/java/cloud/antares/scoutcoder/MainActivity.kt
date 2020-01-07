package cloud.antares.scoutcoder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.text.Normalizer
import java.util.*
import java.util.regex.Pattern


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener, View.OnTouchListener {
    private var selectedCodecs: Int = 0
    private var key1: String = "A"
    private var key2: String = "1"
    private var key1Idx: Int = 0
    private var key2Idx: Int = 0
    private var invertText = false
    private lateinit var internationalAlphabet: List<String>
    private lateinit var italianAlphabet: List<String>
    private lateinit var morseAlphabetAndNumber: List<String>
    private lateinit var morseIndex: List<String>
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    private val TAG = "MainActivity"

    private lateinit var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        MobileAds.initialize(this, getString(R.string.admob_app_id))

        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        mAdView.adListener = object: AdListener() {
            override fun onAdLoaded() {
                Log.d("ADMOB", "onAdLoaded")
            }

            override fun onAdFailedToLoad(errorCode : Int) {
                Log.d("ADMOB", "onAdFailedToLoad(errorCode = $errorCode)")
            }

            override fun onAdOpened() {
                Log.d("ADMOB", "onAdOpened")
            }

            override fun onAdClicked() {
                Log.d("ADMOB", "onAdClicked")
            }

            override fun onAdLeftApplication() {
                Log.d("ADMOB", "onAdLeftApplication")
            }

            override fun onAdClosed() {
                Log.d("ADMOB", "onAdClosed")
            }
        }



        fab.setOnClickListener {
            /*Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()*/
            share()
        }
        fab.isEnabled = false
        fab.imageAlpha = 130

        internationalAlphabet = resources.getStringArray(R.array.international_alphabet_array).asList()
        italianAlphabet = resources.getStringArray(R.array.italian_alphabet_array).asList()
        morseAlphabetAndNumber = resources.getStringArray(R.array.morse_list_array).asList()
        morseIndex = resources.getStringArray(R.array.morse_index).asList()

        conversionType.setOnCheckedChangeListener { group, checkedId ->
            val radioButton: RadioButton = group.findViewById(checkedId)
            val selectedButton: Int = when(radioButton.text.toString()) {
                getString(R.string.morseConversionRadioButton) -> 2
                getString(R.string.numericConversionRadioButton) -> 0
                getString(R.string.alphabeticonversionRadioButton) -> 1
                else -> -1
            }
            selectedCodecs = selectedButton
            populateKeySpinner(selectedButton)
            Log.d("CONVERSION_IDX", ""+selectedButton)


        }

        invertTextSwitch.setOnCheckedChangeListener { _, isChecked ->
            invertText = isChecked
            setKeyResume()
        }

        val idx = getSelectedConversionType()
        selectedCodecs = idx
        key1Spinner.onItemSelectedListener = this
        key2Spinner.onItemSelectedListener = this
        Log.d("CONVERSION_IDX_ORIGINAL",""+idx)
        populateKeySpinner(idx)

        resetButton.setOnClickListener {
            reset()
        }

        encodeButton.setOnClickListener {
            encode()
        }

        encodedText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                fab.isEnabled = s.isNotEmpty()
                fab.imageAlpha = if (s.isNotEmpty()) 255 else 130
                invalidateOptionsMenu()
            }
        })

        key1Spinner.setOnTouchListener(this)
        key2Spinner.setOnTouchListener(this)
        keyResumeTextView.setOnTouchListener(this)
        encodedTextLabel.setOnTouchListener(this)
        originTextLabel.setOnTouchListener(this)
        encodedText.setOnTouchListener(this)
        invertTextSwitch.setOnTouchListener(this)
        key1TextView.setOnTouchListener(this)
        key2TextView.setOnTouchListener(this)
        conversionType.setOnTouchListener(this)
        numericRadioButton.setOnTouchListener(this)
        morseRadioButton.setOnTouchListener(this)
        alphabeticRadioButton.setOnTouchListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        /*val menu_share = menu.findItem(R.id.action_share)
        menu_share.isEnabled = encodedText.text.toString().isNotEmpty()
        menu_share.icon?.alpha = if (encodedText.text.toString().isNotEmpty()) 255 else 130*/
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menu_share = menu?.findItem(R.id.action_share)
        menu_share?.isEnabled = encodedText.text.toString().isNotEmpty()
        menu_share?.icon?.alpha = if (encodedText.text.toString().isNotEmpty()) 255 else 130
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            R.id.action_encode -> {
                encode()
                return true
            }
            R.id.action_reset -> {
                reset()
                return true
            }
            R.id.action_share -> {
                share()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getSelectedConversionType(): Int {
        val radioButtonID: Int = conversionType.checkedRadioButtonId
        return getSelectedConversionType(radioButtonID)

    }

    private fun getSelectedConversionType(radioButtonID: Int): Int {
        val radioButton: RadioButton = conversionType.findViewById(radioButtonID)
        return when(radioButton.text.toString()) {
            getString(R.string.morseConversionRadioButton) -> 2
            getString(R.string.numericConversionRadioButton) -> 0
            getString(R.string.alphabeticonversionRadioButton) -> 1
            else -> -1
        }
    }

    private fun populateKeySpinner(codeType: Int){

        when(codeType){
            0 -> {
                ArrayAdapter.createFromResource(
                    this,
                    R.array.numbers_list_array,
                    android.R.layout.simple_spinner_item
                ).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    key2Spinner.adapter = adapter
                }
            }
            1-> {
                ArrayAdapter.createFromResource(
                    this,
                    R.array.international_alphabet_array,
                    android.R.layout.simple_spinner_item
                ).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    key2Spinner.adapter = adapter
                }
            }
            else -> {
                key1Spinner.visibility = View.GONE
                key2Spinner.visibility = View.GONE
                key1TextView.visibility = View.GONE
                key2TextView.visibility = View.GONE
                keyResumeTextView.visibility = View.GONE
                return
            }
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.international_alphabet_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            key1Spinner.adapter = adapter
        }

        key1Spinner.setSelection(key1Idx, true)
        key2Spinner.setSelection(key2Idx, true)
        key1Spinner.visibility = View.VISIBLE
        key2Spinner.visibility = View.VISIBLE
        key1TextView.visibility = View.VISIBLE
        key2TextView.visibility = View.VISIBLE
        keyResumeTextView.visibility = View.VISIBLE
        encodedText.isEnabled = false
        //keyResumeTextView.text = String.format(getString(R.string.keyResumeTextView),key1,key2)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        parent?.setSelection(0)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        key1 = key1Spinner.selectedItem.toString()
        key2 = key2Spinner.selectedItem.toString()
        key1Idx = key1Spinner.selectedItemPosition
        key2Idx = key2Spinner.selectedItemPosition
        setKeyResume()
    }

    private fun setKeyResume(){
        keyResumeTextView.text = String.format(getString(R.string.keyResumeTextView),key1,key2,
            when(invertText){
                true -> "Invertito"
                false -> "Originale"
            }
        )
    }

    private fun isEnglish(input: String): Boolean {
        return input.contains("j") || input.contains("k") || input.contains("w") || input.contains("x") || input.contains(
            "y"
        )
    }

    private fun characterDistance(
        a: String,
        b: String,
        isEnglish: Boolean
    ): Int {
        val alphabet: List<String> =
            if (isEnglish) internationalAlphabet else italianAlphabet
        return alphabet.indexOf(a) - alphabet.indexOf(b)
    }

    private fun numericCipher(
        key1: String,
        key2: Int,
        invert: Boolean,
        inputText: String
    ): String {
        var input = if (invert) this.reverseString(inputText) else inputText
        input = normalizeString(input)
        val isEnglish =
            isEnglish(input) || isEnglish(key1) || key2 > italianAlphabet.size
        val alphabet: List<String> =
            if (isEnglish) internationalAlphabet else italianAlphabet
        val b = StringBuilder()
        val key =
            (alphabet.size + (key2 - (alphabet.indexOf(key1) + 1))) % alphabet.size
        for (i in input.indices) {
            val curChar = input[i].toString()

            if (curChar.equals(" ")){
                b.append(" - ")
            } else if (!alphabet.contains(curChar)) {
                b.append(curChar)
            } else {
                val indexOfChar = alphabet.indexOf(curChar)
                val cipheredValue = (indexOfChar + key) % alphabet.size + 1
                if (i > 0) {
                    b.append("|")
                }
                b.append(cipheredValue)
            }
        }
        return b.toString()
    }

    private fun alphabeticCipher(
        key1: String,
        key2: String,
        invert: Boolean,
        inputText: String
    ): String {
        var input = if (invert) reverseString(inputText) else inputText
        input = normalizeString(input)
        val isEnglish =
            isEnglish(input) || isEnglish(key1) || isEnglish(key2)
        val charDistance = characterDistance(key1, key2, isEnglish)
        val alphabet: List<String> =
            if (isEnglish) internationalAlphabet else italianAlphabet
        val b = StringBuilder()
        for (element in input) {
            val curChar = element.toString()
            if (!alphabet.contains(curChar)) {
                b.append(curChar)
            } else {
                val cipheredIdx = (alphabet.indexOf(curChar) + charDistance) % alphabet.size
                b.append(alphabet[cipheredIdx])
            }
        }
        return b.toString()
    }

    private fun morseEncoding(inputText: String, invert: Boolean): String {
        var input = if (invert) reverseString(inputText) else inputText
        input = normalizeString(input)
        val b = StringBuilder()
        val punctuations = ".,:;?!"
        for (element in input) {
            if (punctuations.contains(element)){
                b.append(element+"|| ")
            } else if (element.equals(' ',true)) {
                b.append("| ")
            } else if (!morseIndex.contains(element.toString())){
                b.append(element)
            } else {
                val idx = morseIndex.indexOf(element.toString())
                b.append("|"+morseAlphabetAndNumber[idx])
            }
        }
        return b.toString()
    }

    private fun normalizeString(inputStr: String): String {
        var input = inputStr.toUpperCase(Locale.getDefault())
        input = Normalizer.normalize(input, Normalizer.Form.NFKD)
        input = replaceAll(input, "\\p{M}", "'")
        return input
    }

    private fun reverseString(inputText: String): String {
        var reverse = ""
        for (i in inputText.length - 1 downTo 0) {
            reverse = reverse + inputText[i]
        }
        return reverse
    }

    private fun replaceAll(orig:String, regex: String, replacement: String): String {
        return Pattern.compile(regex).matcher(orig).replaceAll(replacement)
    }

    private fun encode() {
        var plainText = originText.text.toString()
        var encText = when(selectedCodecs){
            0 -> numericCipher(key1,key2.toInt(), invertText, plainText)
            1 -> alphabeticCipher(key1, key2,invertText, plainText)
            2 -> morseEncoding(plainText, invertText)
            else -> ""
        }
        encodedText.setText(encText)
        hideKeyboard(this)
    }

    private fun share() {
        hideKeyboard(this)
        val encTextLabel = encodedTextLabel.text.toString()
        val encText = encodedText.text.toString()
        val origTextLabel = originTextLabel.text.toString()
        val origText = originText.text.toString()
        val keyText = when(selectedCodecs) {
            0 -> "[Numerico] " + keyResumeTextView.text.toString()
            1 -> "[Alfabetico] " + keyResumeTextView.text.toString()
            2 -> "[Morse] (" + (if (invertText) "Invertito" else "Normale") + ")"
            else -> ""
        }
        val shareBody = String.format("%s\n## %s\n%s\n## %s\n%s",keyText,origTextLabel,origText,encTextLabel,encText)
        val sharingIntent:Intent = Intent(android.content.Intent.ACTION_SEND)
        sharingIntent.setType("text/plain")
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.share_subject))
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_using)))
    }

    private fun reset() {
        numericRadioButton.isChecked = true
        key1Spinner.setSelection(0)
        key2Spinner.setSelection(0)
        originText.setText("")
        encodedText.setText("")
        hideKeyboard(this)
    }

    private fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        hideKeyboard(this)
        return false
    }
}
