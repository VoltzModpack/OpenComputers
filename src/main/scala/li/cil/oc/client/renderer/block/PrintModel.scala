package li.cil.oc.client.renderer.block

import com.google.common.base.Strings
import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.client.model.ISmartItemModel
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava.bufferAsJavaList
import scala.collection.mutable

object PrintModel extends SmartBlockModelBase with ISmartItemModel {
  override def handleBlockState(state: IBlockState) = state match {
    case extended: IExtendedBlockState => new BlockModel(extended)
    case _ => missingModel
  }

  override def handleItemState(stack: ItemStack) = new ItemModel(stack)

  class BlockModel(val state: IExtendedBlockState) extends SmartBlockModelBase {
    override def getGeneralQuads =
      state.getValue(block.property.PropertyTile.Tile) match {
        case print: tileentity.Print =>
          val faces = mutable.ArrayBuffer.empty[BakedQuad]

          for (shape <- if (print.state) print.data.stateOn else print.data.stateOff if !Strings.isNullOrEmpty(shape.texture)) {
            val bounds = shape.bounds.rotateTowards(print.facing)
            val texture = resolveTexture(shape.texture)
            faces ++= bakeQuads(makeBox(bounds.min, bounds.max), Array.fill(6)(texture), shape.tint.getOrElse(NoTint))
          }

          bufferAsJavaList(faces)
        case _ => super.getGeneralQuads
      }
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val data = new PrintData(stack)

    override def getGeneralQuads = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      Textures.Block.bind()
      val shapes =
        if (data.hasActiveState && KeyBindings.showExtendedTooltips)
          data.stateOn
        else
          data.stateOff
      for (shape <- shapes) {
        val bounds = shape.bounds
        val texture = resolveTexture(shape.texture)
        faces ++= bakeQuads(makeBox(bounds.min, bounds.max), Array.fill(6)(texture), shape.tint.getOrElse(NoTint))
      }
      if (shapes.isEmpty) {
        val bounds = ExtendedAABB.unitBounds
        val texture = resolveTexture(Settings.resourceDomain + ":blocks/white")
        faces ++= bakeQuads(makeBox(bounds.min, bounds.max), Array.fill(6)(texture), Color.rgbValues(EnumDyeColor.LIME))
      }

      bufferAsJavaList(faces)
    }
  }

  private def resolveTexture(name: String) = {
    val texture = Textures.getSprite(name)
    if (texture.getIconName == "missingno") Textures.getSprite("minecraft:blocks/" + name)
    else texture
  }

}