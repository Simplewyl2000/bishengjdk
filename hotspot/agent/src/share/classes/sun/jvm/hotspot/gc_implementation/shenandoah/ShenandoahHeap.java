/*
 * Copyright (c) 2017, 2019, Red Hat, Inc. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package sun.jvm.hotspot.gc_implementation.shenandoah;

import sun.jvm.hotspot.gc_interface.CollectedHeap;
import sun.jvm.hotspot.gc_interface.CollectedHeapName;
import sun.jvm.hotspot.debugger.Address;
import sun.jvm.hotspot.runtime.VM;
import sun.jvm.hotspot.runtime.VMObjectFactory;
import sun.jvm.hotspot.types.Type;
import sun.jvm.hotspot.types.TypeDataBase;
import sun.jvm.hotspot.memory.MemRegion;
import sun.jvm.hotspot.memory.SpaceClosure;
import sun.jvm.hotspot.types.AddressField;
import sun.jvm.hotspot.types.CIntegerField;
import sun.jvm.hotspot.types.JLongField;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class ShenandoahHeap extends CollectedHeap {
    static private CIntegerField numRegions;
    static private JLongField    used;
    static private CIntegerField committed;
    static private AddressField  regions;

    static {
        VM.registerVMInitializedObserver(new Observer() {
            public void update(Observable o, Object data) {
                initialize(VM.getVM().getTypeDataBase());
            }
        });
    }

    static private synchronized void initialize(TypeDataBase db) {
        Type type = db.lookupType("ShenandoahHeap");
        numRegions = type.getCIntegerField("_num_regions");
        used = type.getJLongField("_used");
        committed = type.getCIntegerField("_committed");
        regions = type.getAddressField("_regions");
    }

    @Override
    public CollectedHeapName kind() {
        return CollectedHeapName.SHENANDOAH_HEAP;
    }

    public long numOfRegions() {
        return numRegions.getValue(addr);
    }

    @Override
    public long used() {
        return used.getValue(addr);
    }

    public long committed() {
        return committed.getValue(addr);
    }

    @Override
    public void printOn(PrintStream tty) {
        MemRegion mr = reservedRegion();
        tty.print("Shenandoah heap");
        tty.print(" [" + mr.start() + ", " + mr.end() + "]");
        tty.println(" region size " + ShenandoahHeapRegion.regionSizeBytes() / 1024 + " K");
    }

    public ShenandoahHeap(Address addr) {
        super(addr);
    }

    private ShenandoahHeapRegion at(long index) {
        Address arrayAddr = regions.getValue(addr);
        // Offset of &_regions[index]
        long offset = index * VM.getVM().getAddressSize();
        Address regionAddr = arrayAddr.getAddressAt(offset);
        return (ShenandoahHeapRegion) VMObjectFactory.newObject(ShenandoahHeapRegion.class,
                regionAddr);
    }

    public List/*<MemRegion>*/ getLiveRegions() {
        List res = new ArrayList();
        for (int i = 0; i < numOfRegions(); i++) {
            ShenandoahHeapRegion r = at(i);
            res.add(new MemRegion(r.bottom(), r.top()));
        }
        return res;
    }

}
