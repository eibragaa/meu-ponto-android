package br.com.jean.meuponto.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Mesmo raio do card da lista, para o fundo de exclusão não vazar nas bordas. */
private val DeleteBackgroundShape = RoundedCornerShape(22.dp)

/**
 * Envolve um card com swipe para a direita que revela a ação **Apagar**.
 *
 * O gesto não exclui nada: ao soltar depois do limiar, o conteúdo volta ao lugar e
 * [onDeleteRequest] é chamado uma vez. Quem decide (e confirma) a exclusão é a tela que usa o
 * componente, com diálogo de segunda confirmação. Swipe para a esquerda fica desabilitado e, quando
 * [enabled] é falso, o gesto e a ação de acessibilidade também ficam indisponíveis.
 *
 * Ao cruzar o limiar, o aparelho dá um toque leve: a ação está armada e soltar vai pedir confirmação.
 */
@Composable
fun SwipeToDeleteBox(
    enabled: Boolean,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // O estado guarda o `confirmValueChange` da primeira composição; ler o callback mais recente
    // por `rememberUpdatedState` evita disparar a exclusão do comprovante errado após recompor.
    val currentOnDeleteRequest = rememberUpdatedState(onDeleteRequest)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                currentOnDeleteRequest.value()
            }
            // Sempre volta ao lugar: a confirmação é responsabilidade do diálogo da tela.
            false
        }
    )
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.targetValue) {
        if (state.targetValue == SwipeToDismissBoxValue.StartToEnd) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    val accessibilityModifier = if (enabled) {
        Modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction("Apagar comprovante") {
                    currentOnDeleteRequest.value()
                    true
                }
            )
        }
    } else {
        Modifier
    }

    SwipeToDismissBox(
        state = state,
        modifier = modifier.then(accessibilityModifier),
        enableDismissFromStartToEnd = enabled,
        enableDismissFromEndToStart = false,
        gesturesEnabled = enabled,
        backgroundContent = { DeleteBackground() }
    ) {
        content()
    }
}

/**
 * Fundo vermelho-claro do próprio tema (`errorContainer`) com um círculo de ação à esquerda, no
 * mesmo raio do card. Nada de cor fixa: segue o tema claro e escuro.
 */
@Composable
private fun DeleteBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .clip(DeleteBackgroundShape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                "Apagar",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
