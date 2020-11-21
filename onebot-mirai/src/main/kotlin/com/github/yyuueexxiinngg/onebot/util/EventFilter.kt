package com.github.yyuueexxiinngg.onebot.util

import kotlinx.serialization.json.*
import com.github.yyuueexxiinngg.onebot.PluginBase
import com.github.yyuueexxiinngg.onebot.logger
import java.io.File

class EventFilter {
    companion object {
        private var filter: Filter? = null

        fun init() {
            logger.debug("Initializing event filter...")
            val filterConfigFile = File(PluginBase.configFolder, "filter.json")
            if (filterConfigFile.exists()) {
                logger.info("Found filter.json, trying to initialize event filter...")
                try {
                    val filterConfig = Json.parseToJsonElement(filterConfigFile.readText()).jsonObject
                    filter = constructOperator("and", filterConfig)
                } catch (e: Exception) {
                    logger.warning(e)
                    logger.warning("Error when initializing event filter, event filter will not take effect.")
                }
            }
        }

        fun eval(payload: String): Boolean {
            val json = Json.parseToJsonElement(payload)
            return filter?.eval(json) ?: true
        }

        interface Filter {
            fun eval(payload: JsonElement): Boolean
        }

        class FilterSyntaxError(message: String) : Exception(message)

        class OperationNode(val key: String, val filter: Filter)

        class NotOperator(argument: JsonElement) : Filter {
            private var _operand: Filter

            init {
                if (argument !is JsonObject) {
                    throw FilterSyntaxError("the argument of 'not' operator must be an object")
                }
                _operand = constructOperator("and", argument)
            }

            override fun eval(payload: JsonElement): Boolean {
                return !_operand.eval(payload)
            }
        }

        class AndOperator(argument: JsonElement) : Filter {
            private var _operands: MutableList<OperationNode> = mutableListOf()

            init {
                if (argument !is JsonObject) {
                    throw FilterSyntaxError("the argument of 'and' operator must be an object")
                }

                argument.jsonObject.forEach {
                    val key = it.key
                    val value = it.value

                    if (key.isEmpty()) return@forEach

                    when {
                        key.startsWith(".") -> {
                            // is an operator
                            //   ".foo": {
                            //       "bar": "baz"
                            //   }
                            _operands.add(OperationNode("", constructOperator(key.substring(1), value)))
                        }
                        value is JsonObject -> {
                            // is an normal key with an object as the value
                            //   "foo": {
                            //       ".bar": "baz"
                            //   }
                            _operands.add(OperationNode(key, constructOperator("and", value)))
                        }
                        else -> {
                            // is an normal key with a non-object as the value
                            //   "foo": "bar"
                            _operands.add(OperationNode(key, constructOperator("eq", value)))
                        }
                    }
                }
            }

            override fun eval(payload: JsonElement): Boolean {
                var res = true

                _operands.forEach {
                    res = if (it.key.isEmpty()) {
                        res && it.filter.eval(payload)
                    } else {
                        try {
                            val subPayload = payload.jsonObject[it.key]!!
                            res && it.filter.eval(subPayload)
                        } catch (e: Exception) {
                            false
                        }
                    }

                    if (!res) {
                        return res
                    }
                }

                return res
            }

        }

        class OrOperator(argument: JsonElement) : Filter {
            private var _operands: MutableList<Filter> = mutableListOf()

            init {
                if (argument !is JsonArray) {
                    throw FilterSyntaxError("the argument of 'or' operator must be an array")
                }
                argument.jsonArray.forEach {
                    _operands.add(constructOperator("and", it))
                }
            }

            override fun eval(payload: JsonElement): Boolean {
                var res = false
                _operands.forEach {
                    res = res || it.eval(payload)

                    if (res) return res
                }

                return res
            }

        }

        class EqualOperator(private val argument: JsonElement) : Filter {
            override fun eval(payload: JsonElement): Boolean {
                return argument == payload
            }
        }

        class NotEqualOperator(private val argument: JsonElement) : Filter {
            override fun eval(payload: JsonElement): Boolean {
                return argument != payload
            }
        }

        class InOperator(private val argument: JsonElement) : Filter {
            init {
                if (!(argument is JsonPrimitive || argument is JsonArray)) {
                    throw FilterSyntaxError("the argument of 'in' operator must be a string or an array");
                }
            }

            override fun eval(payload: JsonElement): Boolean {
                if (argument is JsonPrimitive) {
                    return payload is JsonPrimitive && payload.isString && payload.content.contains(argument.content)
                }

                if (argument is JsonArray) {
                    return argument.find { it.jsonPrimitive.content == payload.jsonPrimitive.content } != null
                }

                return false
            }
        }

        class ContainsOperator(private val argument: JsonElement) : Filter {
            init {
                if (!(argument is JsonPrimitive && argument.isString)) {
                    throw FilterSyntaxError("the argument of 'contains' operator must be a string");
                }
            }

            override fun eval(payload: JsonElement): Boolean {
                return if (!(payload is JsonPrimitive && payload.isString)) {
                    false
                } else {
                    payload.content.contains(argument.jsonPrimitive.content)
                }
            }
        }

        class RegexOperator(private val argument: JsonElement) : Filter {
            init {
                if (!(argument is JsonPrimitive && argument.isString)) {
                    throw FilterSyntaxError("the argument of 'regex' operator must be a string");
                }
            }

            override fun eval(payload: JsonElement): Boolean {
                return if (!(payload is JsonPrimitive && payload.isString)) {
                    false
                } else {
                    Regex(argument.jsonPrimitive.content).find(payload.content) != null
                }
            }
        }

        private fun constructOperator(opName: String, argument: JsonElement): Filter {
            return when (opName) {
                "not" -> NotOperator(argument)
                "and" -> AndOperator(argument)
                "or" -> OrOperator(argument)
                "eq" -> EqualOperator(argument)
                "neq" -> NotEqualOperator(argument)
                "in" -> InOperator(argument)
                "contains" -> ContainsOperator(argument)
                "regex" -> RegexOperator(argument)
                else -> throw FilterSyntaxError("the operator `$opName` is not supported")
            }
        }
    }
}