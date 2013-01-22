/*
 * Copyright 2012 frdfsnlght <frdfsnlght@gmail.com>
 * Copyright 2013 James Geboski <jgeboski@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.frdfsnlght.transporter.compatibility;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.frdfsnlght.transporter.Global;
import com.frdfsnlght.transporter.TypeMap;
import com.frdfsnlght.transporter.Utils;

/**
 * @author frdfsnlght <frdfsnlght@gmail.com>
 * @author James Geboski <jgeboski@gmail.com>
 */
public class Compatibility
{
    public void sendAllPacket201PlayerInfo(String playerName, boolean b, int i)
    {
        Object mo;
        Object pk;

        try {
            mo = Global.plugin.getServer();
            mo = Reflect.invoke(mo, "getHandle");
            pk = Reflect.create(Reflect.nmsname("Packet201PlayerInfo"),
                                playerName, b, i);

            Reflect.getMethod(mo, "sendAll", Reflect.nmsname("Packet"))
                   .invoke(pk);
        } catch (ReflectException e) {
            e.printStackTrace();
        }
    }

    public void sendPlayerPacket201PlayerInfo(Player player, String playerName,
                                              boolean b, int i)
    {
        Object mo;
        Object pk;

        try {
            mo = Reflect.invoke(player, "getHandle");
            mo = Reflect.getField(mo, "playerConnection");

            if (mo == null)
                return;

            pk = Reflect.create(Reflect.nmsname("Packet201PlayerInfo"),
                                playerName, true, 9999);

            Reflect.getMethod(mo, "sendAll", Reflect.nmsname("Packet"))
                   .invoke(pk);
        } catch (ReflectException e) {
            e.printStackTrace();
        }
    }

    public ItemStack createItemStack(int type, int amount, short durability)
    {
        return new ItemStack(type, amount, durability);
    }

    public TypeMap getItemStackTag(ItemStack stack)
    {
        Object mo;

        try {
            mo = Reflect.invoke(Reflect.obcname("inventory.CraftItemStack"),
                                "asNMSCopy", stack);

            if (mo == null)
                return null;

            mo = Reflect.invoke(mo, "getTag");
            return (TypeMap) encodeNBT(mo);
        } catch (ReflectException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ItemStack setItemStackTag(ItemStack stack, TypeMap tag)
    {
        Object mo;

        try {
            mo = Reflect.invoke(Reflect.obcname("inventory.CraftItemStack"),
                                "asNMSCopy", stack);

            if (mo == null)
                return null;

            Reflect.invoke(mo, "setTag", decodeNBT(tag));
            mo = Reflect.invoke(Reflect.obcname("inventory.CraftItemStack"),
                                "asCraftMirror", mo);

            return (ItemStack) mo;
        } catch (ReflectException e) {
            e.printStackTrace();
        }

        return null;
    }

    private TypeMap encodeNBT(Object tag)
        throws ReflectException
    {
        TypeMap    map;
        Collection col;
        String     n;

        if (tag == null)
            return null;

        map = new TypeMap();
        col = (Collection) Reflect.invoke(tag, "c");

        for (Object o : col) {
            if (!Reflect.isInstance(o, Reflect.nmsname("NBTBase")))
                continue;

            n = (String) Reflect.invoke(o, "getName");

            if (Reflect.isInstance(o, Reflect.nmsname("NBTTagCompound")))
                map.set(n, encodeNBT(o));
            else
                map.set(n, encodeNBTValue(o));
        }

        return map;
    }

    private Object encodeNBTValue(Object tag)
        throws ReflectException
    {
        List<Object> list;
        List<?>      nlist;

        TypeMap map;
        String  type;

        if (Reflect.isInstance(tag, Reflect.nmsname("NBTTagCompound")))
            return encodeNBT(tag);

        if (Reflect.isInstance(tag, Reflect.nmsname("NBTTagString")))
            return Reflect.getField(tag, "data");

        if (Reflect.isInstance(tag, Reflect.nmsname("NBTTagList"))) {
            list  = new ArrayList<Object>();
            nlist = (ArrayList<?>) Reflect.getField(tag, "list");

            for (Object o : nlist) {
                if (Reflect.isInstance(o, Reflect.nmsname("NBTBase")))
                    list.add(encodeNBTValue(o));
            }

            return list;
        }

        if (Reflect.isInstance(tag, Reflect.nmsname("NBTTagLong")))
            type = "long";
        else if (Reflect.isInstance(tag, Reflect.nmsname("NBTTagInt")))
            type = "int";
        else if (Reflect.isInstance(tag, Reflect.nmsname("NBTTagShort")))
            type = "short";
        else if (Reflect.isInstance(tag, Reflect.nmsname("NBTTagByte")))
            type = "byte";
        else if (Reflect.isInstance(tag, Reflect.nmsname("NBTTagDouble")))
            type = "double";
        else if (Reflect.isInstance(tag, Reflect.nmsname("NBTTagFloat")))
            type = "float";
        else
            return null;

        map = new TypeMap();
        map.set("_type_", type);
        map.put("value",  Reflect.getField(tag, "data"));
        return map;
    }

    private Object decodeNBT(TypeMap map)
        throws ReflectException
    {
        Object tag;
        Object v;

        if (map == null)
            return null;

		tag = Reflect.create(Reflect.nmsname("NBTTagCompound"));

        for (String k : map.getKeys()) {
            v = map.get(k);
            Reflect.invoke(tag, "set", k, decodeNBTValue(v));
        }

        return tag;
    }

    private Object decodeNBTValue(Object value)
        throws ReflectException
    {
        Object  tag;
        TypeMap map;
        String  type;
        Class   ctype;

        if (value instanceof String)
			return Reflect.create(Reflect.nmsname("NBTTagString"), null, value);

        if (value instanceof Collection) {
            tag   = Reflect.create(Reflect.nmsname("NBTTagList"));
            ctype = null;

            for (Object o : (Collection<?>) value) {
                o = decodeNBTValue(o);

				if (ctype == null)
                    ctype = o.getClass();
				else if (ctype != o.getClass())
                    continue;

				Reflect.invoke(tag, "add", o);
			}

			return tag;
        }

        if (!(value instanceof TypeMap))
            return null;

        map  = (TypeMap) value;
        type = map.getString("_type_");

        if (type == null) {
            tag = Reflect.create(Reflect.nmsname("NBTTagCompound"));

            for (String k : map.getKeys())
                Reflect.invoke(tag, "set", k, decodeNBTValue(map.get(k)));

            return tag;
        }

        if (type.equals("long"))
            return Reflect.create(Reflect.nmsname("NBTTagLong"), null,
                                  map.getLong("value"));
        if (type.equals("int"))
            return Reflect.create(Reflect.nmsname("NBTTagInt"), null,
                                  map.getInt("value"));
        if (type.equals("short"))
            return Reflect.create(Reflect.nmsname("NBTTagShort"), null,
                                  map.getShort("value"));
        if (type.equals("byte"))
            return Reflect.create(Reflect.nmsname("NBTTagByte"), null,
                                  map.getByte("value"));
        if (type.equals("double"))
            return Reflect.create(Reflect.nmsname("NBTTagDouble"), null,
                                  map.getDouble("value"));
        if (type.equals("float"))
            return Reflect.create(Reflect.nmsname("NBTTagFloat"), null,
                                  map.getFloat("value"));

        return null;
    }
}
