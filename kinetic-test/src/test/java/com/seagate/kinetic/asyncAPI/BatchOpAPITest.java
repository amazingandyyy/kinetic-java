/**
 * 
 * Copyright (C) 2014 Seagate Technology.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.seagate.kinetic.asyncAPI;

import static com.seagate.kinetic.KineticTestHelpers.buildSuccessOnlyCallbackHandler;
import static com.seagate.kinetic.KineticTestHelpers.toByteArray;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kinetic.client.BatchOperation;
import kinetic.client.CallbackHandler;
import kinetic.client.CallbackResult;
import kinetic.client.ClientConfiguration;
import kinetic.client.Entry;
import kinetic.client.KineticClient;
import kinetic.client.KineticClientFactory;
import kinetic.client.KineticException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.seagate.kinetic.IntegrationTestCase;
import com.seagate.kinetic.KineticTestHelpers;
import com.seagate.kinetic.proto.Kinetic.Command.Status.StatusCode;

/**
 * Kinetic Client batch operation API.
 * <p>
 * Batch operation API include:
 * <p>
 * putAsync(Entry entry, byte[] newVersion, CallbackHandler<Entry> handler)
 * <p>
 * putForcedAsync(Entry entry, CallbackHandler<Entry> handler)
 * <p>
 * deleteAsync(Entry entry, CallbackHandler<Boolean> handler)
 * <p>
 * deleteForcedAsync(byte[] key, CallbackHandler<Boolean> handler)
 * <p>
 * commit()
 * <p>
 * 
 * @see KineticClient
 * @see BatchOperation
 * 
 */

@Test(groups = { "simulator", "drive" })
public class BatchOpAPITest extends IntegrationTestCase {

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_PutsForcedAsyncSucceeds(String clientName) {
        Entry bar = getBarEntry();
        Entry foo = getFooEntry();

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
            batch.putForcedAsync(bar, handler);
        } catch (KineticException e) {
            Assert.fail("Put async throw exception: " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch commit throw exception: " + e.getMessage());
        }

        // get foo, expect to find it
        Entry fooGet = null;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }
        assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
        assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
        assertTrue(Arrays.equals(foo.getEntryMetadata().getVersion(), fooGet
                .getEntryMetadata().getVersion()));

        // get bar, expect to find it
        Entry barGet = null;
        try {
            barGet = getClient(clientName).get(bar.getKey());
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }
        assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
        assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
        assertTrue(Arrays.equals(bar.getEntryMetadata().getVersion(), barGet
                .getEntryMetadata().getVersion()));
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_PutsAsyncSucceeds(String clientName) {
        Entry bar = getBarEntry();
        bar.getEntryMetadata().setVersion(null);
        Entry foo = getFooEntry();
        foo.getEntryMetadata().setVersion(null);
        byte[] newVersion = toByteArray("5678");

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putAsync(foo, newVersion, handler);
            batch.putAsync(bar, newVersion, handler);
        } catch (KineticException e) {
            Assert.fail("Put async throw exception: " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch commit throw exception: " + e.getMessage());
        }

        // get foo, expect to find it
        Entry fooGet = null;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
            assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
            assertTrue(Arrays.equals(newVersion, fooGet.getEntryMetadata()
                    .getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }

        // get bar, expect to find it
        Entry barGet = null;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(newVersion, barGet.getEntryMetadata()
                    .getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions", enabled = false)
    public void testBatchOperation_PutsAsyncOneFailedOnePutAsyncSuccess_AllFailed(
            String clientName) {
        Entry bar = getBarEntry();
        bar.getEntryMetadata().setVersion(null);
        Entry foo = getFooEntry();
        foo.getEntryMetadata().setVersion(null);
        byte[] newVersion = toByteArray("5678");

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putAsync(foo, newVersion, handler);

            bar.getEntryMetadata().setVersion(newVersion);
            batch.putAsync(bar, newVersion, handler);
        } catch (KineticException e) {
            Assert.fail("Put async throw exception: " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.assertTrue(e.getResponseMessage().getCommand().getStatus()
                    .getCode().equals(StatusCode.INVALID_BATCH));
        }

        // get foo, expect to find null
        Entry fooGet = null;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }

        // get bar, expect to find null
        Entry barGet = null;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertNull(barGet);
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_PutsAsyncAndPutForcedAsyncSucceeds(
            String clientName) {
        Entry bar = getBarEntry();
        Entry foo = getFooEntry();
        foo.getEntryMetadata().setVersion(null);
        byte[] newVersion = toByteArray("5678");

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(bar, handler);
            batch.putAsync(foo, newVersion, handler);
        } catch (KineticException e) {
            Assert.fail("Put async throw exception: " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch commit throw exception: " + e.getMessage());
        }

        // get foo, expect to find it
        Entry fooGet = null;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
            assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
            assertTrue(Arrays.equals(newVersion, fooGet.getEntryMetadata()
                    .getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }

        // get bar, expect to find it
        Entry barGet = null;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(bar.getEntryMetadata().getVersion(),
                    barGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions", enabled = false)
    public void testBatchOperation_PutsAsyncOneFailedOnePutForcedAsyncSuccess_AllFailed(
            String clientName) {
        Entry bar = getBarEntry();
        bar.getEntryMetadata().setVersion(null);
        Entry foo = getFooEntry();
        byte[] newVersion = toByteArray("5678");

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);

            bar.getEntryMetadata().setVersion(newVersion);
            batch.putAsync(bar, newVersion, handler);
        } catch (KineticException e) {
            Assert.fail("Put async throw exception: " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.assertTrue(e.getResponseMessage().getCommand().getStatus()
                    .getCode().equals(StatusCode.INVALID_BATCH));
        }

        // get foo, expect to find null
        Entry fooGet = null;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }

        // get bar, expect to find null
        Entry barGet = null;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertNull(barGet);
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_DeletesAsyncSucceeds(String clientName) {
        Entry bar = getBarEntry();
        Entry foo = getFooEntry();
        try {
            getClient(clientName).putForced(bar);
            getClient(clientName).putForced(foo);
        } catch (KineticException e) {
            Assert.fail("Put entry throw exception. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            batch.deleteAsync(bar, dhandler);
            batch.deleteAsync(foo, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async operation throw exception: "
                    + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch commit operation throw exception. "
                    + e.getMessage());
        }

        // get foo, expect to null
        try {
            Entry fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get foo entry throw exception. " + e.getMessage());
        }

        // get bar, expect to null
        try {
            Entry barGet = getClient(clientName).get(bar.getKey());
            assertNull(barGet);
        } catch (KineticException e) {
            Assert.fail("Get bar entry throw exception. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions", enabled = false)
    public void testBatchOperation_DeletesAsyncOneFailed_AllFailed(
            String clientName) {
        Entry bar = getBarEntry();
        Entry foo = getFooEntry();
        try {
            getClient(clientName).putForced(bar);
            getClient(clientName).putForced(foo);
        } catch (KineticException e) {
            Assert.fail("Put entry throw exception. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            bar.getEntryMetadata().setVersion(toByteArray("NoMatchDbVersion"));
            batch.deleteAsync(bar, dhandler);

            batch.deleteAsync(foo, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async operation throw exception: "
                    + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.assertTrue(e.getResponseMessage().getCommand().getStatus()
                    .getCode().equals(StatusCode.INVALID_BATCH));
        }

        // get foo, expect to find it
        Entry fooGet = null;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
            assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
            assertTrue(Arrays.equals(foo.getEntryMetadata().getVersion(),
                    fooGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }

        // get bar, expect to find it
        Entry barGet = null;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(bar.getEntryMetadata().getVersion(),
                    barGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_DeletesForcedAsyncSucceeds(String clientName) {
        Entry bar = getBarEntry();
        Entry foo = getFooEntry();
        try {
            getClient(clientName).putForced(bar);
            getClient(clientName).putForced(foo);
        } catch (KineticException e) {
            Assert.fail("Put entry throw exception. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            batch.deleteForcedAsync(bar.getKey(), dhandler);
            batch.deleteForcedAsync(foo.getKey(), dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async operation throw exception: "
                    + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch commit operation throw exception. "
                    + e.getMessage());
        }

        // get foo, expect to null
        try {
            Entry fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get foo entry throw exception. " + e.getMessage());
        }

        // get bar, expect to null
        try {
            Entry barGet = getClient(clientName).get(bar.getKey());
            assertNull(barGet);
        } catch (KineticException e) {
            Assert.fail("Get bar entry throw exception. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_DeleteForcedAsyncAnddeleteAsyncSucceeds(
            String clientName) {
        Entry bar = getBarEntry();
        Entry foo = getFooEntry();
        try {
            getClient(clientName).putForced(bar);
            getClient(clientName).putForced(foo);
        } catch (KineticException e) {
            Assert.fail("Put entry throw exception. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            batch.deleteAsync(bar, dhandler);
            batch.deleteForcedAsync(foo.getKey(), dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async operation throw exception: "
                    + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch commit operation throw exception. "
                    + e.getMessage());
        }

        // get foo, expect to null
        try {
            Entry fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get foo entry throw exception. " + e.getMessage());
        }

        // get bar, expect to null
        try {
            Entry barGet = getClient(clientName).get(bar.getKey());
            assertNull(barGet);
        } catch (KineticException e) {
            Assert.fail("Get bar entry throw exception. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions", enabled = false)
    public void testBatchOperation_DeletesAsyncOneFailed_OneDeleteForcedAsyncSuccess_AllFailed(
            String clientName) {
        Entry bar = getBarEntry();
        Entry foo = getFooEntry();
        try {
            getClient(clientName).putForced(bar);
            getClient(clientName).putForced(foo);
        } catch (KineticException e) {
            Assert.fail("Put entry throw exception. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            batch.deleteForcedAsync(bar.getKey(), dhandler);

            foo.getEntryMetadata().setVersion(toByteArray("NoMatchDBVersion"));
            batch.deleteAsync(foo, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async operation throw exception: "
                    + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.assertTrue(e.getResponseMessage().getCommand().getStatus()
                    .getCode().equals(StatusCode.INVALID_BATCH));
        }

        // get foo, expect to find it
        Entry fooGet = null;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
            assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
            assertTrue(Arrays.equals(foo.getEntryMetadata().getVersion(),
                    fooGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }

        // get bar, expect to find it
        Entry barGet = null;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(bar.getEntryMetadata().getVersion(),
                    barGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry foo throw exception: " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions", enabled = false)
    public void testBatchOperation_PutAndDeleteSucceeds(String clientName) {
        Entry bar = getBarEntry();

        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put entry failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation throw exception. "
                    + e.getMessage());
        }

        Entry foo = getFooEntry();
        byte[] newVersion = toByteArray("5678");

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
            batch.putAsync(foo, newVersion, handler);
        } catch (KineticException e) {
            Assert.fail("Put entry throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            batch.deleteAsync(bar, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async throw exception. " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch commit throw exception. " + e.getMessage());
        }

        // get foo, expect to find it
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
            assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
            assertTrue(Arrays.equals(newVersion, fooGet.getEntryMetadata()
                    .getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get foo throw exception. " + e.getMessage());
        }

        // get bar, expect to null
        Entry barGet;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertNull(barGet);
        } catch (KineticException e) {
            Assert.fail("Get bar throw exception. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions", enabled = false)
    public void testBatchOperation_PutAndDeleteForcedSucceeds(String clientName) {
        Entry bar = getBarEntry();

        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put entry failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation throw exception. "
                    + e.getMessage());
        }

        Entry foo = getFooEntry();
        byte[] newVersion = toByteArray("5678");

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
            batch.putAsync(foo, newVersion, handler);
        } catch (KineticException e) {
            Assert.fail("Put entry throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            batch.deleteAsync(bar, dhandler);
            batch.deleteForcedAsync(foo.getKey(), dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async throw exception. " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch commit throw exception. " + e.getMessage());
        }

        // get foo, expect to find it
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get foo throw exception. " + e.getMessage());
        }

        // get bar, expect to null
        Entry barGet;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertNull(barGet);
        } catch (KineticException e) {
            Assert.fail("Get bar throw exception. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions", enabled = false)
    public void testBatchOperation_PutAndDeleteForcedPartiallyFailed_AllFailed(
            String clientName) {
        Entry bar = getBarEntry();

        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put entry failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation throw exception. "
                    + e.getMessage());
        }

        Entry foo = getFooEntry();
        byte[] newVersion = toByteArray("5678");

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
            foo.getEntryMetadata().setVersion(toByteArray("NoMatchDbVersion"));
            batch.putAsync(foo, newVersion, handler);

        } catch (KineticException e) {
            Assert.fail("Put entry throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            bar.getEntryMetadata().setVersion(toByteArray("NoMatchDbVersion"));
            batch.deleteAsync(bar, dhandler);
            batch.deleteForcedAsync(foo.getKey(), dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async throw exception. " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.assertTrue(e.getResponseMessage().getCommand().getStatus()
                    .getCode().equals(StatusCode.INVALID_BATCH));
        }

        // get foo, expect to find null
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get foo throw exception. " + e.getMessage());
        }

        // get bar, expect to find it
        Entry barGet;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(bar.getEntryMetadata().getVersion(),
                    barGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get bar throw exception. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions", enabled = false)
    public void testBatchOperation_PutAndDeleteForcedAllOperationFailed_AllFailed(
            String clientName) {
        Entry bar = getBarEntry();

        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put entry failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation throw exception. "
                    + e.getMessage());
        }

        Entry foo = getFooEntry();
        byte[] newVersion = toByteArray("5678");

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            foo.getEntryMetadata().setVersion(toByteArray("NoMatchDbVersion"));
            batch.putAsync(foo, newVersion, handler);

        } catch (KineticException e) {
            Assert.fail("Put entry throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            bar.getEntryMetadata().setVersion(toByteArray("NoMatchDbVersion"));
            batch.deleteAsync(bar, dhandler);

            foo.getEntryMetadata().setVersion(toByteArray("NoMatchDbVersion"));
            batch.deleteAsync(foo, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async throw exception. " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.assertTrue(e.getResponseMessage().getCommand().getStatus()
                    .getCode().equals(StatusCode.INVALID_BATCH));
        }

        // get foo, expect to find null
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get foo throw exception. " + e.getMessage());
        }

        // get bar, expect to find it
        Entry barGet;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(bar.getEntryMetadata().getVersion(),
                    barGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get bar throw exception. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_AllOperationSuccess_AbortOperation_Succeeds(
            String clientName) {
        Entry bar = getBarEntry();

        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put operation failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        Entry foo = getFooEntry();

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
        } catch (KineticException e) {
            Assert.fail("Put operation throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            batch.deleteAsync(bar, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete operation throw exception. " + e.getMessage());
        }

        try {
            batch.abort();
        } catch (KineticException e) {
            Assert.fail("Abort operation throw exception. " + e.getMessage());
        }

        // get foo, expect to null
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }

        // get bar, expect to null
        Entry barGet;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(bar.getEntryMetadata().getVersion(),
                    barGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_PartiallyOperationFailed_AbortOperation_Succeeds(
            String clientName) {
        Entry bar = getBarEntry();

        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put operation failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        Entry foo = getFooEntry();

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
        } catch (KineticException e) {
            Assert.fail("Put operation throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            bar.getEntryMetadata().setVersion(toByteArray("NoMatchDbVersion"));
            batch.deleteAsync(bar, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete operation throw exception. " + e.getMessage());
        }

        try {
            batch.abort();
        } catch (KineticException e) {
            Assert.fail("Abort operation throw exception. " + e.getMessage());
        }

        // get foo, expect to null
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }

        // get bar, expect to find it
        Entry barGet;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(toByteArray("1234"), barGet
                    .getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_AllOperationFailed_AbortOperation_Succeeds(
            String clientName) {
        Entry bar = getBarEntry();

        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put operation failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation failed. " + e.getMessage());
        }

        Entry foo = getFooEntry();

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            byte[] newVersion = toByteArray("5678");
            batch.putAsync(foo, newVersion, handler);
        } catch (KineticException e) {
            Assert.fail("Put operation throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });

            bar.getEntryMetadata().setVersion(toByteArray("NoMatchDbVersion"));
            batch.deleteAsync(bar, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete operation throw exception. " + e.getMessage());
        }

        try {
            batch.abort();
        } catch (KineticException e) {
            Assert.fail("Abort operation throw exception. " + e.getMessage());
        }

        // get foo, expect to null
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertNull(fooGet);
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }

        // get bar, expect to find it
        Entry barGet;
        try {
            barGet = getClient(clientName).get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(toByteArray("1234"), barGet
                    .getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_FollowedBothReadByOneClient_Failed(
            String clientName) {
        Entry bar = getBarEntry();
        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put entry failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation throw exception. "
                    + e.getMessage());
        }

        Entry foo = getFooEntry();

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
        } catch (KineticException e) {
            Assert.fail("Put async throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });
            batch.deleteAsync(bar, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async throw exception. " + e.getMessage());
        }

        ClientConfiguration cc = kineticClientConfigutations.get(clientName);
        KineticClient client1 = null;
        try {
            client1 = KineticClientFactory.createInstance(cc);
        } catch (KineticException e) {
            Assert.fail("Create a new client throw exception. "
                    + e.getMessage());
        }
        try {
            client1.get(foo.getKey());
        } catch (KineticException e) {
            Assert.fail("Another connection can not operate before batch operation end. "
                    + e.getMessage());
        }

        try {
            client1.get(bar.getKey());
        } catch (KineticException e) {
            Assert.fail("Another connection can not operate before batch operation end. "
                    + e.getMessage());
        } finally {
            try {
                client1.close();
            } catch (KineticException e) {
                Assert.fail("Another connection close throw exception. "
                        + e.getMessage());
            }
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch operation commit throw exception. "
                    + e.getMessage());
        }

        // get foo, expect to find it
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
            assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
            assertTrue(Arrays.equals(foo.getEntryMetadata().getVersion(),
                    fooGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }

        // get bar, expect to null
        try {
            assertNull(getClient(clientName).get(bar.getKey()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_FollowedBothReadByOneClientAfterEndBatch_Success(
            String clientName) {
        Entry bar = getBarEntry();
        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put entry failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation throw exception. "
                    + e.getMessage());
        }

        Entry foo = getFooEntry();

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
        } catch (KineticException e) {
            Assert.fail("Put async throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });
            batch.deleteAsync(bar, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async throw exception. " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch operation commit throw exception. "
                    + e.getMessage());
        }

        ClientConfiguration cc = kineticClientConfigutations.get(clientName);
        KineticClient client1 = null;
        try {
            client1 = KineticClientFactory.createInstance(cc);
        } catch (KineticException e) {
            Assert.fail("Create a new client throw exception. "
                    + e.getMessage());
        }
        try {
            client1.get(foo.getKey());
        } catch (KineticException e) {
            Assert.fail("Another connection can not operate before batch operation end. "
                    + e.getMessage());
        }

        try {
            client1.get(bar.getKey());
        } catch (KineticException e) {
            Assert.fail("Another connection can not operate before batch operation end. "
                    + e.getMessage());
        } finally {
            try {
                client1.close();
            } catch (KineticException e) {
                Assert.fail("Another connection close throw exception. "
                        + e.getMessage());
            }
        }

        // get foo, expect to find it
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
            assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
            assertTrue(Arrays.equals(foo.getEntryMetadata().getVersion(),
                    fooGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }

        // get bar, expect to null
        try {
            assertNull(getClient(clientName).get(bar.getKey()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_FollowedBothReadByTwoClientAfterBatchCommit_Success(
            String clientName) {
        Entry bar = getBarEntry();
        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put entry failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation throw exception. "
                    + e.getMessage());
        }

        Entry foo = getFooEntry();

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
        } catch (KineticException e) {
            Assert.fail("Put async throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });
            batch.deleteAsync(bar, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async throw exception. " + e.getMessage());
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch operation commit throw exception. "
                    + e.getMessage());
        }

        ClientConfiguration cc = kineticClientConfigutations.get(clientName);
        KineticClient client1 = null;
        KineticClient client2 = null;
        try {
            client1 = KineticClientFactory.createInstance(cc);
            client2 = KineticClientFactory.createInstance(cc);
        } catch (KineticException e) {
            Assert.fail("Create two clients failed. " + e.getMessage());
        }

        try {
            assertTrue(Arrays.equals(foo.getKey(), client1.get(foo.getKey())
                    .getKey()));
            assertTrue(Arrays.equals(foo.getValue(), client1.get(foo.getKey())
                    .getValue()));
            assertTrue(Arrays.equals(foo.getEntryMetadata().getVersion(),
                    client1.get(foo.getKey()).getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Should not thrown exception. " + e.getMessage());
        } finally {
            try {
                client1.close();
            } catch (KineticException e) {
                Assert.fail("Close connetction failed. " + e.getMessage());
            }
        }

        try {
            assertNull(client2.get(bar.getKey()));
        } catch (KineticException e) {
            Assert.fail("Should not thrown exception. " + e.getMessage());
        } finally {
            try {
                client2.close();
            } catch (KineticException e) {
                Assert.fail("Close connetction failed. " + e.getMessage());
            }
        }

        // get foo, expect to find it
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
            assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
            assertTrue(Arrays.equals(foo.getEntryMetadata().getVersion(),
                    fooGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }

        // get bar, expect to null
        try {
            assertNull(getClient(clientName).get(bar.getKey()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions")
    public void testBatchOperation_Succeeds_FollowedSingleReadByTwoClient_BeforeBatchCommit_Success(
            String clientName) {
        Entry bar = getBarEntry();
        try {
            getClient(clientName).putForced(bar);
        } catch (KineticException e) {
            Assert.fail("Put entry failed. " + e.getMessage());
        }

        BatchOperation batch = null;
        try {
            batch = getClient(clientName).createBatchOperation();
        } catch (KineticException e) {
            Assert.fail("Create batch operation throw exception. "
                    + e.getMessage());
        }

        Entry foo = getFooEntry();

        try {
            CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
                @Override
                public void onSuccess(CallbackResult<Entry> result) {
                }
            });

            batch.putForcedAsync(foo, handler);
        } catch (KineticException e) {
            Assert.fail("Put async throw exception. " + e.getMessage());
        }

        try {
            CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
                @Override
                public void onSuccess(CallbackResult<Boolean> result) {
                }
            });
            batch.deleteAsync(bar, dhandler);
        } catch (KineticException e) {
            Assert.fail("Delete async throw exception. " + e.getMessage());
        }

        ClientConfiguration cc = kineticClientConfigutations.get(clientName);
        KineticClient client1 = null;
        KineticClient client2 = null;
        try {
            client1 = KineticClientFactory.createInstance(cc);
            client2 = KineticClientFactory.createInstance(cc);
        } catch (KineticException e) {
            Assert.fail("Create two clients failed. " + e.getMessage());
        }

        try {
            Entry barGet = client1.get(bar.getKey());
            assertTrue(Arrays.equals(bar.getKey(), barGet.getKey()));
            assertTrue(Arrays.equals(bar.getValue(), barGet.getValue()));
            assertTrue(Arrays.equals(bar.getEntryMetadata().getVersion(),
                    barGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Should not thrown exception. " + e.getMessage());
        } finally {
            try {
                client1.close();
            } catch (KineticException e) {
                Assert.fail("Close connetction failed. " + e.getMessage());
            }
        }

        try {
            assertNull(client2.get(foo.getKey()));
        } catch (KineticException e) {
            Assert.fail("Should not thrown exception. " + e.getMessage());
        } finally {
            try {
                client2.close();
            } catch (KineticException e) {
                Assert.fail("Close connetction failed. " + e.getMessage());
            }
        }

        try {
            batch.commit();
        } catch (KineticException e) {
            Assert.fail("Batch operation commit throw exception. "
                    + e.getMessage());
        }

        // get foo, expect to find it
        Entry fooGet;
        try {
            fooGet = getClient(clientName).get(foo.getKey());
            assertTrue(Arrays.equals(foo.getKey(), fooGet.getKey()));
            assertTrue(Arrays.equals(foo.getValue(), fooGet.getValue()));
            assertTrue(Arrays.equals(foo.getEntryMetadata().getVersion(),
                    fooGet.getEntryMetadata().getVersion()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }

        // get bar, expect to null
        try {
            assertNull(getClient(clientName).get(bar.getKey()));
        } catch (KineticException e) {
            Assert.fail("Get entry failed. " + e.getMessage());
        }
    }

    @Test(dataProvider = "transportProtocolOptions", enabled = false)
    public void testBatchOperation_multiClients_AllSuccess(String clientName)
            throws KineticException, UnsupportedEncodingException,
            InterruptedException {
        int writeThreads = 2;
        CountDownLatch latch = new CountDownLatch(writeThreads);
        ExecutorService pool = Executors.newCachedThreadPool();

        KineticClient kineticClient;
        for (int i = 0; i < writeThreads; i++) {
            kineticClient = KineticClientFactory
                    .createInstance(kineticClientConfigutations.get(clientName));
            pool.execute(new BatchThread(kineticClient, latch));
        }

        // wait all threads finish
        latch.await();
        pool.shutdown();

    }

    private Entry getFooEntry() {
        Entry foo = new Entry();
        byte[] fooKey = toByteArray("foo");
        foo.setKey(fooKey);
        byte[] fooValue = toByteArray("foovalue");
        foo.setValue(fooValue);
        byte[] fooVersion = toByteArray("1234");
        foo.getEntryMetadata().setVersion(fooVersion);

        return foo;
    }

    private Entry getBarEntry() {
        Entry bar = new Entry();
        byte[] barKey = toByteArray("bar");
        bar.setKey(barKey);
        byte[] barValue = toByteArray("barvalue");
        bar.setValue(barValue);
        byte[] barVersion = toByteArray("1234");
        bar.getEntryMetadata().setVersion(barVersion);

        return bar;
    }
}

class BatchThread implements Runnable {
    private final CountDownLatch latch;
    private final KineticClient kineticClient;

    public BatchThread(KineticClient kineticClient, CountDownLatch latch) {
        this.kineticClient = kineticClient;
        this.latch = latch;
    }

    @Override
    public void run() {
        Entry bar = new Entry();
        byte[] barKey = toByteArray("bar");
        bar.setKey(barKey);
        byte[] barValue = toByteArray("barvalue");
        bar.setValue(barValue);
        byte[] barVersion = toByteArray("1234");
        bar.getEntryMetadata().setVersion(barVersion);

        try {
            kineticClient.putForced(bar);
        } catch (KineticException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        BatchOperation batch = null;
        try {
            batch = kineticClient.createBatchOperation();
        } catch (KineticException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        Entry foo = new Entry();
        byte[] fooKey = toByteArray("foo");
        foo.setKey(fooKey);
        byte[] fooValue = toByteArray("foovalue");
        foo.setValue(fooValue);
        byte[] fooVersion = toByteArray("1234");
        foo.getEntryMetadata().setVersion(fooVersion);

        CallbackHandler<Entry> handler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Entry>() {
            @Override
            public void onSuccess(CallbackResult<Entry> result) {
            }
        });

        try {
            batch.putForcedAsync(foo, handler);
        } catch (KineticException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        byte[] barBatchVersion = toByteArray("5678");
        bar.getEntryMetadata().setVersion(barBatchVersion);

        CallbackHandler<Boolean> dhandler = buildSuccessOnlyCallbackHandler(new KineticTestHelpers.SuccessAsyncHandler<Boolean>() {
            @Override
            public void onSuccess(CallbackResult<Boolean> result) {
            }
        });

        try {
            batch.deleteAsync(bar, dhandler);
        } catch (KineticException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            batch.commit();
        } catch (KineticException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try {
            kineticClient.close();
        } catch (KineticException e) {
            Assert.fail("close kineticClient failed, " + e.getMessage());
        } catch (Exception e) {
            Assert.fail("close kineticClient failed, " + e.getMessage());
        }

        // latch count down
        latch.countDown();
    }
}