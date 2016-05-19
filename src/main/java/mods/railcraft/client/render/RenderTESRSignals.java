/*
 * Copyright (c) CovertJaguar, 2015 http://railcraft.info
 *
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */

package mods.railcraft.client.render;

import mods.railcraft.api.core.WorldCoordinate;
import mods.railcraft.api.signals.*;
import mods.railcraft.common.items.ItemGoggles;
import mods.railcraft.common.util.effects.EffectManager;
import mods.railcraft.common.util.misc.EnumColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class RenderTESRSignals extends TileEntitySpecialRenderer<TileEntity> {
    private static final Vec3 CENTER = new Vec3(0.5, 0.5, 0.5);

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks, int destroyStage) {
        if (tile instanceof IControllerTile) {
            if (EffectManager.instance.isGoggleAuraActive(ItemGoggles.GoggleAura.TUNING)) {
                renderPairs(tile, x, y, z, partialTicks, ((IControllerTile) tile).getController(), ColorProfile.RAINBOW);
            } else if (EffectManager.instance.isGoggleAuraActive(ItemGoggles.GoggleAura.SIGNALLING)) {
                renderPairs(tile, x, y, z, partialTicks, ((IControllerTile) tile).getController(), ColorProfile.ASPECT);
            }
        }
        if (tile instanceof ISignalBlockTile) {
            if (EffectManager.instance.isGoggleAuraActive(ItemGoggles.GoggleAura.SURVEYING)) {
                renderPairs(tile, x, y, z, partialTicks, ((ISignalBlockTile) tile).getSignalBlock(), ColorProfile.RAINBOW);
            } else if (EffectManager.instance.isGoggleAuraActive(ItemGoggles.GoggleAura.SIGNALLING)) {
                renderPairs(tile, x, y, z, partialTicks, ((ISignalBlockTile) tile).getSignalBlock(), ColorProfile.BLUE);
            }
        }
        AbstractPair pair = null;
        if (tile instanceof IReceiverTile) {
            pair = ((IReceiverTile) tile).getReceiver();
        } else if (tile instanceof IControllerTile) {
            pair = ((IControllerTile) tile).getController();
        } else if (tile instanceof ISignalBlockTile) {
            pair = ((ISignalBlockTile) tile).getSignalBlock();
        }
        if (pair != null) {
            String name = pair.getName();
            if (name != null) {
                Entity player = Minecraft.getMinecraft().getRenderManager().livingPlayer;
                if (player != null) {
                    final float viewDist = 8f;
                    double dist = player.getDistanceSq(tile.getPos());

                    if (dist <= (double) (viewDist * viewDist)) {
                        MovingObjectPosition mop = player.rayTrace(8, partialTicks);
                        if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK && player.worldObj.getTileEntity(mop.getBlockPos()) == tile) {
                            RenderTools.renderString(name, x + 0.5, y + 1.5, z + 0.5);
                        }
                    }
                }
            }
        }
    }

    private void renderPairs(TileEntity tile, double x, double y, double z, float partialTicks, AbstractPair pair, ColorProfile colorProfile) {
        if (pair.getPairs().isEmpty()) {
            return;
        }
        OpenGL.glPushMatrix();
        OpenGL.glPushAttrib();
        OpenGL.glDisable(GL11.GL_LIGHTING);
        OpenGL.glDisable(GL11.GL_BLEND);
        OpenGL.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        OpenGL.glDisable(GL11.GL_TEXTURE_2D);

        OpenGL.glEnable(GL11.GL_LINE_SMOOTH);
        OpenGL.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        OpenGL.glLineWidth(5F);

        OpenGL.glBegin(GL11.GL_LINES);
        for (WorldCoordinate target : pair.getPairs()) {
            int color = colorProfile.getColor(tile, pair.getCoords(), target);
            float c1 = (float) (color >> 16 & 255) / 255.0F;
            float c2 = (float) (color >> 8 & 255) / 255.0F;
            float c3 = (float) (color & 255) / 255.0F;
            OpenGL.glColor3f(c1, c2, c3);

            OpenGL.glVertex3f((float) x + 0.5f, (float) y + 0.5f, (float) z + 0.5f);
            Vec3 vec = new Vec3(x, y, z).add(CENTER).add(new Vec3(target)).subtract(new Vec3(tile.getPos()));
            OpenGL.glVertex(vec);
        }
        OpenGL.glEnd();

        OpenGL.glPopAttrib();
        OpenGL.glPopMatrix();
    }

    public enum ColorProfile {
        RAINBOW {
            private final WorldCoordinate[] coords = new WorldCoordinate[2];

            @Override
            public int getColor(TileEntity tile, WorldCoordinate source, WorldCoordinate target) {
                coords[0] = source;
                coords[1] = target;
                Arrays.sort(coords);
                return Arrays.hashCode(coords);
            }
        },
        BLUE {
            @Override
            public int getColor(TileEntity tile, WorldCoordinate source, WorldCoordinate target) {
                return EnumColor.BLUE.getHexColor();
            }
        },
        ASPECT {
            @Override
            public int getColor(TileEntity tile, WorldCoordinate source, WorldCoordinate target) {
                if (tile instanceof IControllerTile) {
                    SignalAspect aspect = ((IControllerTile) tile).getController().getAspectFor(target);
                    switch (aspect) {
                        case GREEN:
                            return EnumColor.LIME.getHexColor();
                        case YELLOW:
                        case BLINK_YELLOW:
                            return EnumColor.YELLOW.getHexColor();
                        default:
                            return EnumColor.RED.getHexColor();
                    }
                }
                return BLUE.getColor(tile, source, target);
            }
        };

        public abstract int getColor(TileEntity tile, WorldCoordinate source, WorldCoordinate target);
    }

    protected static void doRenderAspect(RenderFakeBlock.RenderInfo info, TileEntity tile, double x, double y, double z) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        final float depth = 2 * RenderTools.PIXEL;

        OpenGL.glPushMatrix();
        OpenGL.glTranslated(x, y, z);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

//        if (info.brightness < 0) {
//            float light;
//            float lightBottom = 0.5F;
//            if (info.light < 0) {
//                light = 1;
//            } else {
//                light = info.light;
//            }
//            int br;
//            if (info.brightness < 0) {
//                br = info.template.getMixedBrightnessForBlock(tile.getWorld(), tile.getPos());
//            } else {
//                br = info.brightness;
//            }
//            worldRenderer.setBrightness(br);
//            worldRenderer.putColorRGB_F(lightBottom * light, lightBottom * light, lightBottom * light, 0);
//        } else {
//            worldRenderer.setBrightness(info.brightness);
//        }

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 210F, 210F);

        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        if (info.renderSide[2]) {
            worldRenderer.pos(0, 0, depth).tex(info.texture[2].getInterpolatedU(16), info.texture[2].getInterpolatedV(16)).endVertex();
            worldRenderer.pos(0, 1, depth).tex(info.texture[2].getInterpolatedU(16), info.texture[2].getInterpolatedV(0)).endVertex();
            worldRenderer.pos(1, 1, depth).tex(info.texture[2].getInterpolatedU(0), info.texture[2].getInterpolatedV(0)).endVertex();
            worldRenderer.pos(1, 0, depth).tex(info.texture[2].getInterpolatedU(0), info.texture[2].getInterpolatedV(16)).endVertex();
        }
        if (info.renderSide[3]) {
            worldRenderer.pos(0, 0, 1 - depth).tex(info.texture[3].getInterpolatedU(0), info.texture[3].getInterpolatedV(16)).endVertex();
            worldRenderer.pos(1, 0, 1 - depth).tex(info.texture[3].getInterpolatedU(16), info.texture[3].getInterpolatedV(16)).endVertex();
            worldRenderer.pos(1, 1, 1 - depth).tex(info.texture[3].getInterpolatedU(16), info.texture[3].getInterpolatedV(0)).endVertex();
            worldRenderer.pos(0, 1, 1 - depth).tex(info.texture[3].getInterpolatedU(0), info.texture[3].getInterpolatedV(0)).endVertex();
        }
        if (info.renderSide[4]) {
            worldRenderer.pos(depth, 0, 0).tex(info.texture[4].getInterpolatedU(0), info.texture[4].getInterpolatedV(16)).endVertex();
            worldRenderer.pos(depth, 0, 1).tex(info.texture[4].getInterpolatedU(16), info.texture[4].getInterpolatedV(16)).endVertex();
            worldRenderer.pos(depth, 1, 1).tex(info.texture[4].getInterpolatedU(16), info.texture[4].getInterpolatedV(0)).endVertex();
            worldRenderer.pos(depth, 1, 0).tex(info.texture[4].getInterpolatedU(0), info.texture[4].getInterpolatedV(0)).endVertex();
        }
        if (info.renderSide[5]) {
            worldRenderer.pos(1 - depth, 0, 0).tex(info.texture[5].getInterpolatedU(16), info.texture[5].getInterpolatedV(16)).endVertex();
            worldRenderer.pos(1 - depth, 1, 0).tex(info.texture[5].getInterpolatedU(16), info.texture[5].getInterpolatedV(0)).endVertex();
            worldRenderer.pos(1 - depth, 1, 1).tex(info.texture[5].getInterpolatedU(0), info.texture[5].getInterpolatedV(0)).endVertex();
            worldRenderer.pos(1 - depth, 0, 1).tex(info.texture[5].getInterpolatedU(0), info.texture[5].getInterpolatedV(16)).endVertex();
        }

        tessellator.draw();


        OpenGL.glPopMatrix();
    }
}
