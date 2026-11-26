package com.fortunesimulator.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.random.Random

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("fortune_local", MODE_PRIVATE) }
    private var balance = 0
    private var bet = 10
    private var game = "TIGRE"
    private var spins = 0
    private var wins = 0
    private lateinit var balanceView: TextView
    private lateinit var resultView: TextView
    private lateinit var reelsView: TextView
    private lateinit var statsView: TextView
    private lateinit var betView: TextView

    private val gold = Color.rgb(255, 211, 91)
    private val dark = Color.rgb(71, 8, 17)
    private val symbols = listOf("🐯", "🐰", "🐂", "💎", "7️⃣", "⭐")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        balance = prefs.getInt("balance", 1000)
        spins = prefs.getInt("spins", 0)
        wins = prefs.getInt("wins", 0)
        buildUi()
    }

    private fun save() {
        prefs.edit().putInt("balance", balance).putInt("spins", spins).putInt("wins", wins).apply()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(18, 20, 18, 18)
            setBackgroundColor(dark)
        }
        val title = text("FORTUNE SIMULATOR", 25, gold, true)
        root.addView(title)
        root.addView(text("SIMULADOR OFFLINE • CRÉDITOS FICTÍCIOS", 11, Color.WHITE, false))
        balanceView = text("", 24, gold, true)
        balanceView.setPadding(0, 24, 0, 14)
        root.addView(balanceView)
        val gameRow = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        listOf("TIGRE" to "🐯", "COELHO" to "🐰", "TOURO" to "🐂").forEach { (name, emoji) ->
            val b = Button(this).apply { text = "$emoji\n$name"; setOnClickListener { game = name; resultView.text = "Jogo selecionado: $game"; update() } }
            gameRow.addView(b, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(gameRow)
        reelsView = text("🐯     💎     ⭐", 36, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(125, 16, 28))
            setPadding(8, 35, 8, 35)
        }
        val reelParams = LinearLayout.LayoutParams(-1, -2)
        reelParams.setMargins(0, 22, 0, 12)
        root.addView(reelsView, reelParams)
        resultView = text("Escolha sua aposta e toque em GIRAR.", 16, Color.WHITE, true)
        root.addView(resultView)
        betView = text("Aposta: 10 créditos", 16, gold, true)
        betView.setPadding(0, 12, 0, 4)
        root.addView(betView)
        val bets = LinearLayout(this).apply { gravity = Gravity.CENTER }
        listOf(1, 5, 10, 50, 100).forEach { amount ->
            val b = Button(this).apply { text = amount.toString(); setOnClickListener { bet = amount; update() } }
            bets.addView(b, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(bets)
        val spin = Button(this).apply {
            text = "🎰  GIRAR"
            textSize = 21f
            setTextColor(dark)
            setBackgroundColor(gold)
            setOnClickListener { spin() }
        }
        val spinParams = LinearLayout.LayoutParams(-1, 64)
        spinParams.setMargins(0, 12, 0, 8)
        root.addView(spin, spinParams)
        val recharge = Button(this).apply { text = "＋ ADICIONAR CRÉDITOS"; setOnClickListener { rechargeDialog() } }
        root.addView(recharge, LinearLayout.LayoutParams(-1, -2))
        val reset = Button(this).apply { text = "REINICIAR SALDO"; setOnClickListener { resetDialog() } }
        root.addView(reset, LinearLayout.LayoutParams(-1, -2))
        statsView = text("", 13, Color.LTGRAY, false).apply { setPadding(0, 14, 0, 0) }
        root.addView(statsView)
        root.addView(text("Somente entretenimento. Sem dinheiro real, depósitos ou saques.", 10, Color.LTGRAY, false))
        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
        update()
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value; textSize = size.toFloat(); setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }

    private fun update() {
        balanceView.text = "SALDO: %,d créditos".format(balance)
        betView.text = "Aposta: $bet créditos"
        statsView.text = "Jogadas: $spins    Vitórias: $wins"
    }

    private fun spin() {
        if (balance < bet) {
            resultView.text = "Créditos insuficientes. Use Adicionar Créditos."
            return
        }
        balance -= bet
        spins++
        val icons = symbols.shuffled().take(3).toMutableList()
        // Cada rodada é independente; chances ilustrativas e ajustadas para simulação.
        val roll = Random.nextInt(100)
        if (roll < 9) icons[1] = icons[0]
        if (roll < 4) icons[2] = icons[0]
        reelsView.text = icons.joinToString("       ")
        val multiplier = when {
            icons.distinct().size == 1 -> 10
            icons[0] == icons[1] || icons[1] == icons[2] || icons[0] == icons[2] -> 2
            else -> 0
        }
        if (multiplier > 0) {
            val prize = bet * multiplier
            balance += prize
            wins++
            resultView.text = "🎉 VITÓRIA! +$prize créditos ($multiplier×)"
        } else resultView.text = "Não foi dessa vez. Tente novamente!"
        save()
        update()
    }

    private fun rechargeDialog() {
        val choices = arrayOf("+1.000 créditos", "+5.000 créditos", "+10.000 créditos", "Quantidade personalizada")
        AlertDialog.Builder(this).setTitle("Adicionar créditos fictícios")
            .setItems(choices) { _, which ->
                if (which < 3) addCredits(listOf(1000, 5000, 10000)[which])
                else customRecharge()
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun customRecharge() {
        val input = EditText(this).apply {
            inputType = 2
            hint = "Ex.: 2500"
        }
        AlertDialog.Builder(this).setTitle("Quantidade de créditos")
            .setView(input).setPositiveButton("Adicionar") { _, _ ->
                val amount = input.text.toString().toIntOrNull()
                if (amount == null || amount <= 0) Toast.makeText(this, "Digite uma quantidade válida.", Toast.LENGTH_SHORT).show()
                else addCredits(amount)
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun addCredits(amount: Int) {
        balance = (balance.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        save(); update()
        Toast.makeText(this, "+$amount créditos fictícios adicionados!", Toast.LENGTH_SHORT).show()
    }

    private fun resetDialog() {
        AlertDialog.Builder(this).setTitle("Reiniciar saldo?")
            .setMessage("O saldo voltará para 1.000 créditos. O histórico de jogadas também será zerado.")
            .setPositiveButton("Reiniciar") { _, _ ->
                balance = 1000; spins = 0; wins = 0
                save(); update()
                reelsView.text = "🐯     💎     ⭐"
                resultView.text = "Saldo reiniciado!"
            }.setNegativeButton("Cancelar", null).show()
    }
}
