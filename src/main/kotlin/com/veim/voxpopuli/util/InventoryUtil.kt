package com.veim.voxpopuli.util

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

object InventoryUtil {
	private val LOGGER = HytaleLogger.forEnclosingClass()

	private val warnedKeys: MutableSet<String> =
		Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

	/**
	 * Best-effort inventory check.
	 *
	 * Returns:
	 * - true  -> item found
	 * - false -> item not found
	 * - null  -> inventory could not be inspected (API mismatch / unexpected structure)
	 */
	fun playerHasItemId(
		store: Store<EntityStore>,
		ref: Ref<EntityStore>,
		requiredItemId: String,
	): Boolean? {
		val itemId = requiredItemId.trim()
		if (itemId.isEmpty()) return null

		val player = store.getComponent(ref, Player.getComponentType()) ?: return null

		val inventoryRoot = findInventoryRoot(player)
			?: return warnOnce("no-inventory-root:$itemId") { null }

		return try {
			containsItemId(inventoryRoot, itemId)
		} catch (e: Exception) {
			warnOnce("inventory-scan-failed:$itemId") {
				LOGGER.at(Level.WARNING).withCause(e).log("[VoxPopuli] Failed to scan inventory for itemId=%s", itemId)
				null
			}
		}
	}

	private fun findInventoryRoot(player: Any): Any? {
		// Common patterns: player.inventory, player.getInventory(), player.playerData.inventory, etc.
		val direct = readMember(player, "inventory") ?: callNoArg(player, "getInventory")
		if (direct != null) return direct

		val playerData = readMember(player, "playerData") ?: callNoArg(player, "getPlayerData")
		if (playerData != null) {
			val nested = readMember(playerData, "inventory") ?: callNoArg(playerData, "getInventory")
			if (nested != null) return nested
		}

		return null
	}

	private fun containsItemId(root: Any, requiredItemId: String): Boolean {
		val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
		val queue: ArrayDeque<Pair<Any, Int>> = ArrayDeque()
		queue.add(root to 0)

		val maxDepth = 8
		while (queue.isNotEmpty()) {
			val (obj, depth) = queue.removeFirst()
			if (!visited.add(obj)) continue
			if (depth > maxDepth) continue

			val id = tryExtractId(obj)
			if (id != null && id == requiredItemId) return true

			when (obj) {
				is Map<*, *> -> {
					obj.values.forEach { v ->
						if (v != null) queue.add(v to (depth + 1))
					}
					continue
				}
				is Iterable<*> -> {
					obj.forEach { v ->
						if (v != null) queue.add(v to (depth + 1))
					}
					continue
				}
				is Array<*> -> {
					obj.forEach { v ->
						if (v != null) queue.add(v to (depth + 1))
					}
					continue
				}
			}

			// Avoid traversing common leaf types.
			if (obj is String || obj is Number || obj is Boolean || obj.javaClass.isEnum) continue

			val clazz = obj.javaClass
			val pkg = clazz.`package`?.name.orEmpty()
			// Keep traversal focused on game/server objects.
			if (!pkg.startsWith("com.hypixel.hytale") && !pkg.startsWith("com.veim.voxpopuli")) continue

			clazz.declaredFields.forEach { field ->
				if (Modifier.isStatic(field.modifiers)) return@forEach
				if (field.type.isPrimitive) return@forEach
				field.isAccessible = true
				val value = field.get(obj) ?: return@forEach
				queue.add(value to (depth + 1))
			}
		}

		return false
	}

	private fun tryExtractId(obj: Any): String? {
		// Try common getters first.
		callNoArg(obj, "getId")?.let { if (it is String) return it }
		callNoArg(obj, "getItemId")?.let { if (it is String) return it }
		callNoArg(obj, "id")?.let { if (it is String) return it }

		// Then fields.
		readMember(obj, "id")?.let { if (it is String) return it }
		readMember(obj, "Id")?.let { if (it is String) return it }
		readMember(obj, "itemId")?.let { if (it is String) return it }
		readMember(obj, "registryId")?.let { if (it is String) return it }

		return null
	}

	private fun readMember(target: Any, name: String): Any? {
		return try {
			val field = target.javaClass.declaredFields.firstOrNull { it.name == name } ?: return null
			field.isAccessible = true
			field.get(target)
		} catch (_: Exception) {
			null
		}
	}

	private fun callNoArg(target: Any, methodName: String): Any? {
		return try {
			val method = target.javaClass.methods.firstOrNull {
				it.name == methodName && it.parameterCount == 0
			} ?: return null
			method.invoke(target)
		} catch (_: Exception) {
			null
		}
	}

	private inline fun <T> warnOnce(key: String, block: () -> T): T {
		if (warnedKeys.add(key)) {
			LOGGER.at(Level.WARNING).log(
				"[VoxPopuli] Inventory requirement check is enabled, but inventory API could not be inspected (key=%s).", key
			)
		}
		return block()
	}
}
